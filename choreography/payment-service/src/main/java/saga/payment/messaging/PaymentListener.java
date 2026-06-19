package saga.payment.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import saga.payment.config.RabbitConfig;
import saga.payment.domain.BalanceEntity;
import saga.payment.domain.BalanceRepository;
import saga.payment.domain.PaymentEntity;
import saga.payment.domain.PaymentRepository;
import saga.payment.model.SagaMessages.OrderCreatedEvent;
import saga.payment.model.SagaMessages.PaymentResultEvent;
import saga.payment.model.SagaMessages.StockResultEvent;

@Component
@RabbitListener(queues = RabbitConfig.PAYMENT_EVENTS_QUEUE)
public class PaymentListener {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitHandler
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        double amount = event.price() * event.quantity();
        System.out.println("[Payment Service] Received OrderCreatedEvent for Saga: " + event.sagaId() + ", User: " + event.userId() + ", Amount: $" + amount);

        try {
            BalanceEntity balance = balanceRepository.findById(event.userId())
                    .orElse(new BalanceEntity(event.userId(), 0.0));

            if (balance.getBalance() >= amount) {
                balance.setBalance(balance.getBalance() - amount);
                balanceRepository.save(balance);

                PaymentEntity payment = new PaymentEntity(
                        event.orderId(),
                        event.userId(),
                        amount,
                        "SUCCESS",
                        event.sagaId()
                );
                paymentRepository.save(payment);

                System.out.println("[Payment Service] SUCCESS: Reserved $" + amount + " for user '" + event.userId() + "'. New balance: $" + balance.getBalance());
                
                PaymentResultEvent successResponse = new PaymentResultEvent(
                        event.sagaId(),
                        event.orderId(),
                        event.userId(),
                        event.itemId(),
                        event.quantity(),
                        event.price(),
                        true,
                        "Payment reserved successfully"
                );
                rabbitTemplate.convertAndSend(RabbitConfig.CHOREOGRAPHY_EXCHANGE, "payment.event.success", successResponse);
            } else {
                PaymentEntity payment = new PaymentEntity(
                        event.orderId(),
                        event.userId(),
                        amount,
                        "FAILED",
                        event.sagaId()
                );
                paymentRepository.save(payment);

                System.out.println("[Payment Service] FAILED: Insufficient funds for user '" + event.userId() + "'. Current balance: $" + balance.getBalance());

                PaymentResultEvent failedResponse = new PaymentResultEvent(
                        event.sagaId(),
                        event.orderId(),
                        event.userId(),
                        event.itemId(),
                        event.quantity(),
                        event.price(),
                        false,
                        "Insufficient funds"
                );
                rabbitTemplate.convertAndSend(RabbitConfig.CHOREOGRAPHY_EXCHANGE, "payment.event.failed", failedResponse);
            }
        } catch (Exception e) {
            System.err.println("[Payment Service] Failed to process payment reservation: " + e.getMessage());
            PaymentResultEvent failedResponse = new PaymentResultEvent(
                    event.sagaId(),
                    event.orderId(),
                    event.userId(),
                    event.itemId(),
                    event.quantity(),
                    event.price(),
                    false,
                    e.getMessage()
            );
            rabbitTemplate.convertAndSend(RabbitConfig.CHOREOGRAPHY_EXCHANGE, "payment.event.failed", failedResponse);
        }
    }

    @RabbitHandler
    @Transactional
    public void handleStockFailed(StockResultEvent event) {
        System.out.println("[Payment Service] Received StockResultEvent (Failure) for Saga: " + event.sagaId() + ". Triggering compensation/refund.");
        
        paymentRepository.findBySagaId(event.sagaId()).ifPresent(payment -> {
            if ("SUCCESS".equals(payment.getStatus())) {
                balanceRepository.findById(payment.getUserId()).ifPresent(balance -> {
                    balance.setBalance(balance.getBalance() + payment.getAmount());
                    balanceRepository.save(balance);

                    payment.setStatus("REFUNDED");
                    paymentRepository.save(payment);

                    System.out.println("[Payment Service] REFUNDED: Refunded $" + payment.getAmount() + " to user '" + payment.getUserId() + "'. New balance: $" + balance.getBalance());
                });
            } else {
                System.out.println("[Payment Service] Refund skipped, payment status is: " + payment.getStatus());
            }
        });
    }
}

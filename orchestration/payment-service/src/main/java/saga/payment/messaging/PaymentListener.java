package saga.payment.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import saga.payment.domain.*;
import saga.payment.model.SagaMessages.*;

@Component
@RabbitListener(queues = "payment-commands-queue")
public class PaymentListener {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitHandler
    @Transactional
    public void handleReservePayment(ReservePaymentCommand cmd) {
        System.out.println("[Payment Service] Received ReservePaymentCommand for User: " + cmd.userId() + ", Amount: $" + cmd.amount());

        try {
            BalanceEntity balance = balanceRepository.findById(cmd.userId())
                    .orElse(new BalanceEntity(cmd.userId(), 0.0));

            if (balance.getBalance() >= cmd.amount()) {
                balance.setBalance(balance.getBalance() - cmd.amount());
                balanceRepository.save(balance);

                PaymentEntity payment = new PaymentEntity(
                        cmd.orderId(),
                        cmd.userId(),
                        cmd.amount(),
                        "SUCCESS",
                        cmd.sagaId()
                );
                paymentRepository.save(payment);

                System.out.println("[Payment Service] SUCCESS: Reserved $" + cmd.amount() + " for user '" + cmd.userId() + "'. New balance: $" + balance.getBalance());
                
                PaymentResultEvent response = new PaymentResultEvent(cmd.sagaId(), true, "Payment reserved successfully");
                rabbitTemplate.convertAndSend("saga-exchange", "saga.event.payment", response);
            } else {
                PaymentEntity payment = new PaymentEntity(
                        cmd.orderId(),
                        cmd.userId(),
                        cmd.amount(),
                        "FAILED",
                        cmd.sagaId()
                );
                paymentRepository.save(payment);

                System.out.println("[Payment Service] FAILED: Insufficient funds for user '" + cmd.userId() + "'. Current balance: $" + balance.getBalance());

                PaymentResultEvent response = new PaymentResultEvent(cmd.sagaId(), false, "Insufficient funds");
                rabbitTemplate.convertAndSend("saga-exchange", "saga.event.payment", response);
            }
        } catch (Exception e) {
            System.err.println("[Payment Service] Failed to process payment reservation: " + e.getMessage());
            PaymentResultEvent response = new PaymentResultEvent(cmd.sagaId(), false, e.getMessage());
            rabbitTemplate.convertAndSend("saga-exchange", "saga.event.payment", response);
        }
    }

    @RabbitHandler
    @Transactional
    public void handleRefundPayment(RefundPaymentCommand cmd) {
        System.out.println("[Payment Service] Received RefundPaymentCommand (Compensation) for Saga: " + cmd.sagaId());
        
        paymentRepository.findBySagaId(cmd.sagaId()).ifPresent(payment -> {
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

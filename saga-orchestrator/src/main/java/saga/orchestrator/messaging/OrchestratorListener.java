package saga.orchestrator.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import saga.orchestrator.config.RabbitConfig;
import saga.orchestrator.domain.SagaStateEntity;
import saga.orchestrator.domain.SagaStateRepository;
import saga.orchestrator.model.SagaMessages.*;

@Component
@RabbitListener(queues = RabbitConfig.ORCHESTRATOR_QUEUE)
public class OrchestratorListener {

    @Autowired
    private SagaStateRepository sagaRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitHandler
    public void handleOrderCreated(OrderCreatedEvent event) {
        System.out.println("[Orchestrator] Received OrderCreatedEvent for saga: " + event.sagaId() + ", OrderID: " + event.orderId());
        
        sagaRepository.findById(event.sagaId()).ifPresent(saga -> {
            if (event.success()) {
                saga.setStatus("ORDER_CREATED");
                saga.setOrderId(event.orderId());
                sagaRepository.save(saga);

                // Progress to Step 2: Reserve Payment Funds
                System.out.println("[Orchestrator] Step 1 (Create Order) succeeded. Advancing to Step 2: Reserve Funds...");
                ReservePaymentCommand reserveCmd = new ReservePaymentCommand(
                        saga.getId(),
                        event.orderId(),
                        saga.getUserId(),
                        saga.getPrice()
                );
                rabbitTemplate.convertAndSend(RabbitConfig.SAGA_EXCHANGE, "payment.command.reserve", reserveCmd);
            } else {
                saga.setStatus("FAILED");
                saga.setErrorMessage(event.message());
                sagaRepository.save(saga);
                System.out.println("[Orchestrator] Saga FAILED at step 1: " + event.message());
            }
        });
    }

    @RabbitHandler
    public void handlePaymentResult(PaymentResultEvent event) {
        System.out.println("[Orchestrator] Received PaymentResultEvent for saga: " + event.sagaId() + ", Success: " + event.success());

        sagaRepository.findById(event.sagaId()).ifPresent(saga -> {
            if (event.success()) {
                saga.setStatus("PAYMENT_RESERVED");
                sagaRepository.save(saga);

                // Progress to Step 3: Reserve Inventory Stock
                System.out.println("[Orchestrator] Step 2 (Reserve Funds) succeeded. Advancing to Step 3: Reserve Stock...");
                ReserveStockCommand reserveStockCmd = new ReserveStockCommand(
                        saga.getId(),
                        saga.getOrderId(),
                        saga.getItemId(),
                        saga.getQuantity()
                );
                rabbitTemplate.convertAndSend(RabbitConfig.SAGA_EXCHANGE, "stock.command.reserve", reserveStockCmd);
            } else {
                System.out.println("[Orchestrator] Step 2 (Reserve Funds) FAILED! Initiating Rollback...");
                saga.setStatus("COMPENSATING");
                saga.setErrorMessage(event.message());
                sagaRepository.save(saga);

                // Compensate Step 1: Cancel Order
                CancelOrderCommand cancelOrderCmd = new CancelOrderCommand(saga.getId(), saga.getOrderId());
                rabbitTemplate.convertAndSend(RabbitConfig.SAGA_EXCHANGE, "order.command.cancel", cancelOrderCmd);
            }
        });
    }

    @RabbitHandler
    public void handleStockResult(StockResultEvent event) {
        System.out.println("[Orchestrator] Received StockResultEvent for saga: " + event.sagaId() + ", Success: " + event.success());

        sagaRepository.findById(event.sagaId()).ifPresent(saga -> {
            if (event.success()) {
                saga.setStatus("STOCK_RESERVED");
                sagaRepository.save(saga);

                // Step 4: Confirm Order (Final commit)
                System.out.println("[Orchestrator] Step 3 (Reserve Stock) succeeded. Advancing to Step 4: Confirm Order...");
                ConfirmOrderCommand confirmOrderCmd = new ConfirmOrderCommand(saga.getId(), saga.getOrderId());
                rabbitTemplate.convertAndSend(RabbitConfig.SAGA_EXCHANGE, "order.command.confirm", confirmOrderCmd);
                
                // Complete Saga
                saga.setStatus("COMPLETED");
                sagaRepository.save(saga);
                System.out.println("[Orchestrator] --- SAGA COMPLETED SUCCESSFULLY ---");
            } else {
                System.out.println("[Orchestrator] Step 3 (Reserve Stock) FAILED! Initiating Rollback...");
                saga.setStatus("COMPENSATING");
                saga.setErrorMessage(event.message());
                sagaRepository.save(saga);

                // Compensate Step 2: Refund Payment
                RefundPaymentCommand refundPaymentCmd = new RefundPaymentCommand(
                        saga.getId(),
                        saga.getOrderId(),
                        saga.getUserId(),
                        saga.getPrice()
                );
                rabbitTemplate.convertAndSend(RabbitConfig.SAGA_EXCHANGE, "payment.command.refund", refundPaymentCmd);

                // Compensate Step 1: Cancel Order
                CancelOrderCommand cancelOrderCmd = new CancelOrderCommand(saga.getId(), saga.getOrderId());
                rabbitTemplate.convertAndSend(RabbitConfig.SAGA_EXCHANGE, "order.command.cancel", cancelOrderCmd);
            }
        });
    }
}

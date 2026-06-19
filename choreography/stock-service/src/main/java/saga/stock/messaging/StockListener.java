package saga.stock.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import saga.stock.config.RabbitConfig;
import saga.stock.domain.InventoryEntity;
import saga.stock.domain.InventoryRepository;
import saga.stock.domain.StockReservationEntity;
import saga.stock.domain.StockReservationRepository;
import saga.stock.model.SagaMessages.PaymentResultEvent;
import saga.stock.model.SagaMessages.StockResultEvent;

@Component
@RabbitListener(queues = RabbitConfig.STOCK_EVENTS_QUEUE)
public class StockListener {

    @Autowired
    private StockReservationRepository reservationRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitHandler
    @Transactional
    public void handlePaymentReserved(PaymentResultEvent event) {
        System.out.println("[Stock Service] Received PaymentResultEvent (Success) for Saga: " + event.sagaId() + ", Item: " + event.itemId() + ", Quantity: " + event.quantity());

        try {
            InventoryEntity item = inventoryRepository.findById(event.itemId())
                    .orElse(new InventoryEntity(event.itemId(), 0));

            if (item.getQuantity() >= event.quantity()) {
                item.setQuantity(item.getQuantity() - event.quantity());
                inventoryRepository.save(item);

                StockReservationEntity reservation = new StockReservationEntity(
                        event.orderId(),
                        event.itemId(),
                        event.quantity(),
                        "RESERVED",
                        event.sagaId()
                );
                reservationRepository.save(reservation);

                System.out.println("[Stock Service] SUCCESS: Reserved " + event.quantity() + " unit(s) of '" + event.itemId() + "'. Remaining stock: " + item.getQuantity());

                StockResultEvent successResponse = new StockResultEvent(
                        event.sagaId(),
                        event.orderId(),
                        event.userId(),
                        event.itemId(),
                        event.quantity(),
                        event.price(),
                        true,
                        "Stock reserved successfully"
                );
                rabbitTemplate.convertAndSend(RabbitConfig.CHOREOGRAPHY_EXCHANGE, "stock.event.success", successResponse);
            } else {
                StockReservationEntity reservation = new StockReservationEntity(
                        event.orderId(),
                        event.itemId(),
                        event.quantity(),
                        "FAILED",
                        event.sagaId()
                );
                reservationRepository.save(reservation);

                System.out.println("[Stock Service] FAILED: Out of stock for '" + event.itemId() + "'. Current stock: " + item.getQuantity());

                StockResultEvent failedResponse = new StockResultEvent(
                        event.sagaId(),
                        event.orderId(),
                        event.userId(),
                        event.itemId(),
                        event.quantity(),
                        event.price(),
                        false,
                        "Out of stock"
                );
                rabbitTemplate.convertAndSend(RabbitConfig.CHOREOGRAPHY_EXCHANGE, "stock.event.failed", failedResponse);
            }
        } catch (Exception e) {
            System.err.println("[Stock Service] Failed to reserve stock: " + e.getMessage());
            StockResultEvent failedResponse = new StockResultEvent(
                    event.sagaId(),
                    event.orderId(),
                    event.userId(),
                    event.itemId(),
                    event.quantity(),
                    event.price(),
                    false,
                    e.getMessage()
            );
            rabbitTemplate.convertAndSend(RabbitConfig.CHOREOGRAPHY_EXCHANGE, "stock.event.failed", failedResponse);
        }
    }
}

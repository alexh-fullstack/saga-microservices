package saga.stock.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import saga.stock.domain.*;
import saga.stock.model.SagaMessages.*;

@Component
@RabbitListener(queues = "stock-commands-queue")
public class StockListener {

    @Autowired
    private StockReservationRepository reservationRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitHandler
    @Transactional
    public void handleReserveStock(ReserveStockCommand cmd) {
        System.out.println("[Stock Service] Received ReserveStockCommand for Item: " + cmd.itemId() + ", Quantity: " + cmd.quantity());

        try {
            InventoryEntity item = inventoryRepository.findById(cmd.itemId())
                    .orElse(new InventoryEntity(cmd.itemId(), 0));

            if (item.getQuantity() >= cmd.quantity()) {
                item.setQuantity(item.getQuantity() - cmd.quantity());
                inventoryRepository.save(item);

                StockReservationEntity reservation = new StockReservationEntity(
                        cmd.orderId(),
                        cmd.itemId(),
                        cmd.quantity(),
                        "RESERVED",
                        cmd.sagaId()
                );
                reservationRepository.save(reservation);

                System.out.println("[Stock Service] SUCCESS: Reserved " + cmd.quantity() + " unit(s) of '" + cmd.itemId() + "'. Remaining stock: " + item.getQuantity());

                StockResultEvent response = new StockResultEvent(cmd.sagaId(), true, "Stock reserved successfully");
                rabbitTemplate.convertAndSend("saga-exchange", "saga.event.stock", response);
            } else {
                StockReservationEntity reservation = new StockReservationEntity(
                        cmd.orderId(),
                        cmd.itemId(),
                        cmd.quantity(),
                        "FAILED",
                        cmd.sagaId()
                );
                reservationRepository.save(reservation);

                System.out.println("[Stock Service] FAILED: Out of stock for '" + cmd.itemId() + "'. Current stock: " + item.getQuantity());

                StockResultEvent response = new StockResultEvent(cmd.sagaId(), false, "Out of stock");
                rabbitTemplate.convertAndSend("saga-exchange", "saga.event.stock", response);
            }
        } catch (Exception e) {
            System.err.println("[Stock Service] Failed to reserve stock: " + e.getMessage());
            StockResultEvent response = new StockResultEvent(cmd.sagaId(), false, e.getMessage());
            rabbitTemplate.convertAndSend("saga-exchange", "saga.event.stock", response);
        }
    }

    @RabbitHandler
    @Transactional
    public void handleReleaseStock(ReleaseStockCommand cmd) {
        System.out.println("[Stock Service] Received ReleaseStockCommand (Compensation) for Saga: " + cmd.sagaId());

        reservationRepository.findBySagaId(cmd.sagaId()).ifPresent(reservation -> {
            if ("RESERVED".equals(reservation.getStatus())) {
                inventoryRepository.findById(reservation.getItemId()).ifPresent(item -> {
                    item.setQuantity(item.getQuantity() + reservation.getQuantity());
                    inventoryRepository.save(item);

                    reservation.setStatus("RELEASED");
                    reservationRepository.save(reservation);

                    System.out.println("[Stock Service] RELEASED: Returned " + reservation.getQuantity() + " unit(s) of '" + reservation.getItemId() + "' to inventory. New stock: " + item.getQuantity());
                });
            } else {
                System.out.println("[Stock Service] Stock release skipped, reservation status is: " + reservation.getStatus());
            }
        });
    }
}

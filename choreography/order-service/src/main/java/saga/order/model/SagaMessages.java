package saga.order.model;

import java.util.UUID;

public class SagaMessages {
    public record OrderCreatedEvent(
            UUID sagaId, 
            UUID orderId, 
            String userId, 
            String itemId, 
            int quantity, 
            double price
    ) {}

    public record PaymentResultEvent(
            UUID sagaId, 
            UUID orderId, 
            String userId, 
            String itemId, 
            int quantity, 
            double price, 
            boolean success, 
            String message
    ) {}

    public record StockResultEvent(
            UUID sagaId, 
            UUID orderId, 
            String userId, 
            String itemId, 
            int quantity, 
            double price, 
            boolean success, 
            String message
    ) {}
}

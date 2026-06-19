package saga.order.model;

import java.util.UUID;

public class SagaMessages {
    public record CreateOrderCommand(UUID sagaId, String userId, String itemId, int quantity, double price) {}
    public record ConfirmOrderCommand(UUID sagaId, UUID orderId) {}
    public record CancelOrderCommand(UUID sagaId, UUID orderId) {}
    public record OrderCreatedEvent(UUID sagaId, UUID orderId, boolean success, String message) {}
}

package saga.orchestrator.model;

import java.util.UUID;

public class SagaMessages {
    public record CreateOrderCommand(UUID sagaId, String userId, String itemId, int quantity, double price) {}
    public record ConfirmOrderCommand(UUID sagaId, UUID orderId) {}
    public record CancelOrderCommand(UUID sagaId, UUID orderId) {}
    public record OrderCreatedEvent(UUID sagaId, UUID orderId, boolean success, String message) {}

    public record ReservePaymentCommand(UUID sagaId, UUID orderId, String userId, double amount) {}
    public record RefundPaymentCommand(UUID sagaId, UUID orderId, String userId, double amount) {}
    public record PaymentResultEvent(UUID sagaId, boolean success, String message) {}

    public record ReserveStockCommand(UUID sagaId, UUID orderId, String itemId, int quantity) {}
    public record ReleaseStockCommand(UUID sagaId, UUID orderId, String itemId, int quantity) {}
    public record StockResultEvent(UUID sagaId, boolean success, String message) {}
}

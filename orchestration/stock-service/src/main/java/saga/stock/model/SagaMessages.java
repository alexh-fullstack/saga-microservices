package saga.stock.model;

import java.util.UUID;

public class SagaMessages {
    public record ReserveStockCommand(UUID sagaId, UUID orderId, String itemId, int quantity) {}
    public record ReleaseStockCommand(UUID sagaId, UUID orderId, String itemId, int quantity) {}
    public record StockResultEvent(UUID sagaId, boolean success, String message) {}
}

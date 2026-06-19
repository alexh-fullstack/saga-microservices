package saga.payment.model;

import java.util.UUID;

public class SagaMessages {
    public record ReservePaymentCommand(UUID sagaId, UUID orderId, String userId, double amount) {}
    public record RefundPaymentCommand(UUID sagaId, UUID orderId, String userId, double amount) {}
    public record PaymentResultEvent(UUID sagaId, boolean success, String message) {}
}

package saga.payment.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class PaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    private UUID orderId;
    private String userId;
    private double amount;
    private String status;
    private UUID sagaId;

    public PaymentEntity() {}

    public PaymentEntity(UUID orderId, String userId, double amount, String status, UUID sagaId) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.sagaId = sagaId;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public UUID getSagaId() { return sagaId; }
    public void setSagaId(UUID sagaId) { this.sagaId = sagaId; }
}

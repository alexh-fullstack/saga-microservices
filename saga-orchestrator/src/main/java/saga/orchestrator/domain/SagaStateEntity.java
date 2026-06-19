package saga.orchestrator.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sagas")
public class SagaStateEntity {
    @Id
    private UUID id;
    
    private String userId;
    private String itemId;
    private int quantity;
    private double price;
    private String status;
    
    private UUID orderId;
    private String paymentId;
    private String reservationId;
    private String errorMessage;
    private Instant createdAt = Instant.now();

    // Default constructor for JPA
    public SagaStateEntity() {}

    public SagaStateEntity(UUID id, String userId, String itemId, int quantity, double price, String status) {
        this.id = id;
        this.userId = userId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.price = price;
        this.status = status;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

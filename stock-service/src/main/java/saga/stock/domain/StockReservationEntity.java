package saga.stock.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "reservations")
public class StockReservationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    private UUID orderId;
    private String itemId;
    private int quantity;
    private String status;
    private UUID sagaId;

    public StockReservationEntity() {}

    public StockReservationEntity(UUID orderId, String itemId, int quantity, String status, UUID sagaId) {
        this.orderId = orderId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.status = status;
        this.sagaId = sagaId;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public UUID getSagaId() { return sagaId; }
    public void setSagaId(UUID sagaId) { this.sagaId = sagaId; }
}

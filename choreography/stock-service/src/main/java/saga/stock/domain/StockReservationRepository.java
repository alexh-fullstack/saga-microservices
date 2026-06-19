package saga.stock.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface StockReservationRepository extends JpaRepository<StockReservationEntity, UUID> {
    Optional<StockReservationEntity> findBySagaId(UUID sagaId);
}

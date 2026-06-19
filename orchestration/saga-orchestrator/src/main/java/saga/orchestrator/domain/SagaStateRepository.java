package saga.orchestrator.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SagaStateRepository extends JpaRepository<SagaStateEntity, UUID> {
}

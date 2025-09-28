package auth_service.auth.repository;

import auth_service.auth.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    Page<AuditEvent> findByActorUserIdOrTargetUserUserId(UUID actorId, UUID targetId, Pageable pageable);
}

package auth_service.auth.service;

import auth_service.auth.entity.AuditEvent;
import auth_service.auth.entity.AuditEventType;
import auth_service.auth.entity.User;
import auth_service.auth.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    @Transactional
    public void logEvent(AuditEventType eventType, User actor, User targetUser, String ipAddress, String details) {
        AuditEvent event = AuditEvent.builder()
                .eventType(eventType)
                .actor(actor)
                .targetUser(targetUser)
                .ipAddress(ipAddress)
                .details(details)
                .build();
        auditEventRepository.save(event);
    }
}

package auth_service.auth.dto;

import auth_service.auth.entity.Role;
import auth_service.auth.entity.UserStatus; // <-- Добавлен импорт
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;


@Data
@Builder
public class UserAdminViewDto {
    private UUID userId;
    private String name;
    private String email;
    private Set<Role> roles;
    private UserStatus status;
    private String statusReason;
    private boolean isVerified;
    private Instant createdAt;
    private List<SessionInfoResponse> activeSessions;
}


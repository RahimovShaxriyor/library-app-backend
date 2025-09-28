package auth_service.auth.dto;

import auth_service.auth.entity.Role;
import auth_service.auth.entity.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class UserAdminSummaryDto {
    private UUID userId;
    private String name;
    private String email;
    private Set<Role> roles;
    private UserStatus status;
    private Instant createdAt;
}


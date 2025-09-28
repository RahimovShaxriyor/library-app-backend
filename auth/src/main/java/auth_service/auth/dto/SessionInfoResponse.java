package auth_service.auth.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;


@Builder
public record SessionInfoResponse(
        UUID sessionId,
        String ipAddress,
        String deviceName,
        Instant lastUsedAt,
        boolean isCurrentSession
) {}

package auth_service.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh токен не может быть пустым")
        String refreshToken
) {}

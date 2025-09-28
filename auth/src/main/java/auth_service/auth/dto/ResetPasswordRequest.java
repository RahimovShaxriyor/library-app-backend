package auth_service.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Токен не может быть пустым")
        String token,

        @NotBlank(message = "Новый пароль не может быть пустым")
        @Size(min = 8, message = "Пароль должен содержать минимум 8 символов")
        String newPassword
) {}

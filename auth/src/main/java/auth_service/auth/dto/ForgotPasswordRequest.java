package auth_service.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "Email не может быть пустым")
        @Email(message = "Некорректный формат email")
        String email
) {}

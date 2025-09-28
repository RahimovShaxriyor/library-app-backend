package auth_service.auth.dto;

import auth_service.auth.entity.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class UpdateUserStatusRequest {

    @NotNull(message = "Статус не может быть пустым")
    private UserStatus status;

    private String reason;
}

package auth_service.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private UUID userId;
    private String name;
    private String email;
    private Set<String> roles;
    private boolean isVerified;
}

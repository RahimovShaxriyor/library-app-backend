package auth_service.auth.controller;

import auth_service.auth.dto.UserDto;
import auth_service.auth.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

/**
 * Контроллер для эндпоинтов, связанных с текущим аутентифицированным пользователем.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {


    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal User currentUser) {
        UserDto userDto = new UserDto(
                currentUser.getUserId(),
                currentUser.getName(),
                currentUser.getEmail(),
                currentUser.getRoles().stream().map(Enum::name).collect(Collectors.toSet()),
                currentUser.isVerified()
        );
        return ResponseEntity.ok(userDto);
    }
}

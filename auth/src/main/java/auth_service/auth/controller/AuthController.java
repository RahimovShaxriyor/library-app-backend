package auth_service.auth.controller;

import auth_service.auth.dto.*;
import auth_service.auth.entity.User;
import auth_service.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(new ApiResponse(true, "Регистрация прошла успешно. Пожалуйста, подтвердите ваш email."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(authService.login(request, servletRequest));
    }


    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest servletRequest) {
        // Используем record accessor `request.refreshToken()`
        return ResponseEntity.ok(authService.refreshToken(request.refreshToken(), servletRequest));
    }


    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(new ApiResponse(true, "Успешный выход из системы."));
    }


    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        // Используем record accessor `request.email()`
        authService.forgotPassword(request.email());
        return ResponseEntity.ok(new ApiResponse(true, "Если пользователь с таким email существует, ему будет отправлена ссылка для сброса пароля."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(new ApiResponse(true, "Пароль успешно изменен."));
    }


    @GetMapping("/sessions")
    public ResponseEntity<List<SessionInfoResponse>> getUserSessions(@AuthenticationPrincipal User currentUser) {
        List<SessionInfoResponse> sessions = authService.getUserSessions(currentUser.getEmail(), null);
        return ResponseEntity.ok(sessions);
    }


    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse> terminateSession(@AuthenticationPrincipal User currentUser, @PathVariable UUID sessionId) {
        authService.terminateSession(currentUser.getEmail(), sessionId);
        return ResponseEntity.ok(new ApiResponse(true, "Сессия успешно завершена."));
    }
}


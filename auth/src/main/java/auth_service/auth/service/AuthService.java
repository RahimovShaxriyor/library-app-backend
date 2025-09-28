package auth_service.auth.service;

import auth_service.auth.dto.AuthResponse;
import auth_service.auth.dto.LoginRequest;
import auth_service.auth.dto.RegisterRequest;
import auth_service.auth.dto.SessionInfoResponse;
import auth_service.auth.entity.*;
import auth_service.auth.exception.TokenRefreshException;
import auth_service.auth.exception.UserAlreadyExistsException;
import auth_service.auth.repository.PasswordResetTokenRepository;
import auth_service.auth.repository.RefreshTokenRepository;
import auth_service.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ua_parser.Client;
import ua_parser.Parser;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final Parser uaParser;
    private final AuditService auditService;


    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new UserAlreadyExistsException("Пользователь с email " + request.email() + " уже существует.");
        }
        var user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .roles(Set.of(Role.USER))
                .build();
        User savedUser = userRepository.save(user);


        auditService.logEvent(AuditEventType.USER_REGISTERED, null, savedUser, getCurrentRequestIpAddress(), "Новый пользователь успешно зарегистрирован.");

        var accessToken = jwtService.generateAccessToken(savedUser);
        return new AuthResponse(accessToken, null);
    }


    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        String ipAddress = getIpAddress(servletRequest);
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
            User user = (User) authentication.getPrincipal();


            auditService.logEvent(AuditEventType.USER_LOGIN_SUCCESS, user, user, ipAddress, "Успешный вход в систему.");

            return createSession(user, servletRequest);
        } catch (Exception e) {

            userRepository.findByEmail(request.email()).ifPresent(user ->
                    auditService.logEvent(AuditEventType.USER_LOGIN_FAILURE, user, user, ipAddress, "Неудачная попытка входа. Причина: " + e.getMessage())
            );
            throw e;
        }
    }


    @Transactional
    public AuthResponse refreshToken(String oldRefreshToken, HttpServletRequest servletRequest) {
        String ipAddress = getIpAddress(servletRequest);
        String deviceName = getDeviceName(servletRequest);

        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(oldRefreshToken, ipAddress, deviceName);
        User user = newRefreshToken.getUser();
        String newAccessToken = jwtService.generateAccessToken(user);

        return new AuthResponse(newAccessToken, newRefreshToken.getToken());
    }


    @Transactional
    public void logout(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        String accessToken = authHeader.substring(7);
        String userEmail = jwtService.extractUsername(accessToken);

        userRepository.findByEmail(userEmail).ifPresent(user -> {
            tokenBlacklistService.blacklistToken(accessToken, jwtService.extractClaim(accessToken, Claims::getExpiration));
            refreshTokenService.deleteAllTokensForUser(user.getUserId());
            auditService.logEvent(AuditEventType.USER_LOGOUT, user, user, getIpAddress(request), "Пользователь вышел из системы.");
        });
    }


    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            PasswordResetToken token = new PasswordResetToken(user);
            passwordResetTokenRepository.save(token);
            auditService.logEvent(AuditEventType.PASSWORD_RESET_REQUESTED, user, user, getCurrentRequestIpAddress(), "Запрошен сброс пароля.");
            System.out.println("Password Reset Token for " + user.getEmail() + ": " + token.getToken());
        });
    }


    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenRefreshException(token, "Токен для сброса пароля не найден."));

        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            throw new TokenRefreshException(token, "Срок действия токена для сброса пароля истек.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
        refreshTokenService.deleteAllTokensForUser(user.getUserId());
        auditService.logEvent(AuditEventType.PASSWORD_RESET_SUCCESS, user, user, getCurrentRequestIpAddress(), "Пароль успешно сброшен.");
    }

    public List<SessionInfoResponse> getUserSessions(String userEmail, String currentToken) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден."));
        List<RefreshToken> sessions = refreshTokenRepository.findAllByUser_UserId(user.getUserId());

        return sessions.stream().map(session -> SessionInfoResponse.builder()
                .sessionId(session.getId())
                .ipAddress(session.getIpAddress())
                .deviceName(session.getDeviceName())
                .lastUsedAt(session.getLastUsedAt())
                .isCurrentSession(Objects.equals(session.getToken(), currentToken))
                .build()
        ).collect(Collectors.toList());
    }

    @Transactional
    public void terminateSession(String userEmail, UUID sessionId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден."));
        RefreshToken session = refreshTokenRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Сессия не найдена."));

        if (!session.getUser().getUserId().equals(user.getUserId())) {
            throw new SecurityException("Попытка удаления чужой сессии.");
        }
        refreshTokenRepository.delete(session);
    }

    private AuthResponse createSession(User user, HttpServletRequest request) {
        String ipAddress = getIpAddress(request);
        String deviceName = getDeviceName(request);
        var refreshToken = refreshTokenService.createRefreshToken(user, ipAddress, deviceName);
        var accessToken = jwtService.generateAccessToken(user);
        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String getDeviceName(HttpServletRequest request) {
        String userAgentHeader = request.getHeader("User-Agent");
        if (userAgentHeader == null) return "Unknown Device";
        Client c = uaParser.parse(userAgentHeader);
        return c.os.family + " on " + c.userAgent.family;
    }


    private String getCurrentRequestIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return getIpAddress(attributes.getRequest());
        } catch (IllegalStateException e) {
            return "N/A";
        }
    }
}


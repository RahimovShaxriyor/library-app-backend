package auth_service.auth.service;

import auth_service.auth.entity.RefreshToken;
import auth_service.auth.entity.User;
import auth_service.auth.exception.TokenRefreshException;
import auth_service.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiration-ms}")
    private Long refreshTokenDurationMs;


    @Transactional
    public RefreshToken createRefreshToken(User user, String ipAddress, String deviceName) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .ipAddress(ipAddress)
                .deviceName(deviceName)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }


    private RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh токен просрочен. Пожалуйста, войдите в систему снова.");
        }
        return token;
    }


    @Transactional
    public RefreshToken rotateRefreshToken(String oldTokenString, String ipAddress, String deviceName) {
        RefreshToken oldToken = refreshTokenRepository.findByToken(oldTokenString)
                .map(this::verifyExpiration)
                .orElseThrow(() -> new TokenRefreshException(oldTokenString, "Refresh токен не найден в базе данных!"));

        User user = oldToken.getUser();
        refreshTokenRepository.delete(oldToken);

        return createRefreshToken(user, ipAddress, deviceName);
    }


    @Transactional
    public void deleteAllTokensForUser(UUID userId) {
        refreshTokenRepository.deleteAllByUser_UserId(userId);
    }

}


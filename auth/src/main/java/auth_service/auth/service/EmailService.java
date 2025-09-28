package auth_service.auth.service;

import auth_service.auth.entity.User;

/**
 * Интерфейс для сервиса отправки email-сообщений.
 */
public interface EmailService {
    void sendPasswordResetEmail(User user, String token);
}

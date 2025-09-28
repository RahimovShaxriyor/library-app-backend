package auth_service.auth.service.impl;

import auth_service.auth.entity.User;
import auth_service.auth.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Override
    public void sendPasswordResetEmail(User user, String token) {
        String resetUrl = "http://your-frontend-url/reset-password?token=" + token;

        String message = String.format(
                "Здравствуйте, %s!\n\n" +
                        "Вы запросили сброс пароля. Пожалуйста, перейдите по ссылке ниже, чтобы установить новый пароль:\n%s\n\n" +
                        "Если вы не запрашивали сброс, просто проигнорируйте это письмо.\n",
                user.getName(), resetUrl
        );

        logger.info("---- EMAIL ДЛЯ СБРОСА ПАРОЛЯ ----");
        logger.info("Кому: {}", user.getEmail());
        logger.info("Тема: Сброс пароля");
        logger.info("Тело письма:\n{}", message);
        logger.info("---------------------------------");
    }
}

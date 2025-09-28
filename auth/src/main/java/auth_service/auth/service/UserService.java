package auth_service.auth.service;

import auth_service.auth.entity.PasswordResetToken;
import auth_service.auth.entity.User;
import auth_service.auth.repository.PasswordResetTokenRepository;
import auth_service.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;


    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Transactional
    public void createPasswordResetTokenForUser(String email) {
        User user = findByEmail(email);
        PasswordResetToken resetToken = new PasswordResetToken(user);
        tokenRepository.save(resetToken);
        System.out.println("Password reset token created: " + resetToken.getToken());
    }

    @Transactional
    public void changeUserPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid password reset token"));

        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            throw new RuntimeException("Expired password reset token");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenRepository.delete(resetToken);
    }
}
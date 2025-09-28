package auth_service.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Сущность, представляющая Refresh Token.
 * Используется для долгосрочного хранения сессий пользователей и обновления access-токенов.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


    @Column(nullable = false, unique = true, length = 512)
    private String token;


    @ManyToOne(fetch = FetchType.LAZY) // LAZY-загрузка для лучшей производительности
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    @Column(nullable = false)
    private String ipAddress;


    @Column(nullable = false)
    private String deviceName;


    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;


    @UpdateTimestamp
    private Instant lastUsedAt;

    @Column(nullable = false)
    private Instant expiryDate;
}

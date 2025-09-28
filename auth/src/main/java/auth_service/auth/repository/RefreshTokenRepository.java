package auth_service.auth.repository;

import auth_service.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    /**
     * ИСПРАВЛЕНО: Имя метода изменено с findAllByUserId на findAllByUser_UserId.
     * Знак подчеркивания (_) указывает Spring Data JPA, что нужно зайти в связанную сущность 'user'
     * и искать по ее полю 'userId'.
     */
    List<RefreshToken> findAllByUser_UserId(UUID userId);

    /**
     * Удаляет все refresh-токены для указанного пользователя.
     */
    @Modifying
    void deleteAllByUser_UserId(UUID userId);
}


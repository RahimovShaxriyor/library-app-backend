package auth_service.auth.service;

import auth_service.auth.dto.SessionInfoResponse;
import auth_service.auth.dto.UpdateUserStatusRequest;
import auth_service.auth.dto.UserAdminSummaryDto;
import auth_service.auth.dto.UserAdminViewDto;
import auth_service.auth.entity.*;
import auth_service.auth.repository.RefreshTokenRepository;
import auth_service.auth.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService; // Интеграция сервиса аудита


    @Transactional(readOnly = true)
    public Page<UserAdminSummaryDto> getAllUsers(Pageable pageable, String email, Set<Role> roles) {
        Specification<User> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (email != null && !email.isBlank()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> userPage = userRepository.findAll(spec, pageable);
        return userPage.map(this::convertToSummaryDto);
    }


    @Transactional(readOnly = true)
    public UserAdminViewDto getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь с ID " + userId + " не найден"));

        List<RefreshToken> sessions = refreshTokenRepository.findAllByUser_UserId(user.getUserId());
        List<SessionInfoResponse> sessionDtos = sessions.stream()
                .map(session -> SessionInfoResponse.builder()
                        .sessionId(session.getId())
                        .ipAddress(session.getIpAddress())
                        .deviceName(session.getDeviceName())
                        .lastUsedAt(session.getLastUsedAt())
                        .isCurrentSession(false) // В админ-панели это поле не так важно
                        .build())
                .collect(Collectors.toList());

        return convertToViewDto(user, sessionDtos);
    }


    @Transactional
    public void updateUserStatus(UUID targetUserId, UpdateUserStatusRequest request, User adminUser) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь с ID " + targetUserId + " не найден"));

        targetUser.setStatus(request.getStatus());
        targetUser.setStatusReason(request.getReason());
        userRepository.save(targetUser);

        if (request.getStatus() == UserStatus.SUSPENDED) {
            refreshTokenService.deleteAllTokensForUser(targetUserId);
        }

        String details = String.format("Статус пользователя изменен на %s. Причина: %s", request.getStatus(), request.getReason());
        auditService.logEvent(AuditEventType.ADMIN_USER_STATUS_CHANGED, adminUser, targetUser, getCurrentRequestIpAddress(), details);
    }



    private UserAdminSummaryDto convertToSummaryDto(User user) {
        return UserAdminSummaryDto.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .roles(user.getRoles())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private UserAdminViewDto convertToViewDto(User user, List<SessionInfoResponse> sessions) {
        return UserAdminViewDto.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .roles(user.getRoles())
                .status(user.getStatus())
                .statusReason(user.getStatusReason())
                .createdAt(user.getCreatedAt())
                .isVerified(user.isVerified())
                .activeSessions(sessions)
                .build();
    }

    private String getCurrentRequestIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            return ip;
        } catch (IllegalStateException e) {
            return "N/A";
        }
    }
}


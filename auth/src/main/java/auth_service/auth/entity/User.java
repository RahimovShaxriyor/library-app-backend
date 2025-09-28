package auth_service.auth.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сущность, представляющая пользователя в системе.
 * Реализует UserDetails для интеграции со Spring Security.
 */
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID userId;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name="user_roles", joinColumns=@JoinColumn(name="user_id"))
    @Column(name="role", nullable = false)
    private Set<Role> roles;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    private String statusReason;

    @Column(nullable = false)
    private boolean verified = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;


    public User() {
    }

    private User(UserBuilder builder) {
        this.name = builder.name;
        this.email = builder.email;
        this.password = builder.password;
        this.roles = builder.roles;
        this.status = builder.status;
        this.verified = builder.verified;
        this.statusReason = builder.statusReason;
    }

    // --- Spring Security UserDetails implementation ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.status != UserStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.status == UserStatus.ACTIVE && this.verified;
    }

    // --- Getters and Setters ---

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // --- Builder, equals, hashCode, toString ---

    public static UserBuilder builder() {
        return new UserBuilder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId) && Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, email);
    }

    public static class UserBuilder {
        private String name;
        private String email;
        private String password;
        private Set<Role> roles;
        private UserStatus status = UserStatus.PENDING_VERIFICATION; // Значение по умолчанию
        private boolean verified = false; // Значение по умолчанию
        private String statusReason;

        public UserBuilder name(String name) { this.name = name; return this; }
        public UserBuilder email(String email) { this.email = email; return this; }
        public UserBuilder password(String password) { this.password = password; return this; }
        public UserBuilder roles(Set<Role> roles) { this.roles = roles; return this; }
        public UserBuilder status(UserStatus status) { this.status = status; return this; }
        public UserBuilder verified(boolean verified) { this.verified = verified; return this; }
        public UserBuilder statusReason(String statusReason) { this.statusReason = statusReason; return this; }

        public User build() {
            return new User(this);
        }
    }
}


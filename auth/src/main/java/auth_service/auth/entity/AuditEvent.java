package auth_service.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditEventType eventType;


    @ManyToOne
    @JoinColumn(name = "actor_id")
    private User actor;


    @ManyToOne
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @Column(nullable = false)
    private String ipAddress;

    @Column(length = 512)
    private String details;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}

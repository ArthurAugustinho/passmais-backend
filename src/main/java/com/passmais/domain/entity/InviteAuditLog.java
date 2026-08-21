package com.passmais.domain.entity;

import com.passmais.domain.enums.InviteAuditStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "invite_audit_logs")
public class InviteAuditLog {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invite_code_id", nullable = false)
    private TeamInvite invite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_by_secretary_id")
    private User secretary;

    @CreationTimestamp
    @Column(name = "used_at", nullable = false, updatable = false)
    private Instant usedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InviteAuditStatus status;

    @Column(length = 200)
    private String details;
}

package com.passmais.domain.entity;

import com.passmais.domain.enums.TeamInviteStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "team_invites")
public class TeamInvite {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    @Column(name = "invite_code_hash", nullable = false, length = 128, unique = true)
    private String inviteCodeHash;

    @Column(nullable = false)
    private int maxUses;

    @Column(name = "uses_remaining", nullable = false)
    private int usesRemaining;

    @Column(name = "display_code", length = 20)
    private String displayCode;

    @Column(name = "secretary_full_name", nullable = false, length = 255)
    private String secretaryFullName;

    @Column(name = "secretary_corporate_email", nullable = false, length = 255)
    private String secretaryCorporateEmail;

    @Column(nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TeamInviteStatus status = TeamInviteStatus.ACTIVE;

    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default
    private int failedAttempts = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public boolean isExpired(Instant reference) {
        return expiresAt != null && expiresAt.isBefore(reference);
    }

    public boolean hasRemainingUses() {
        return usesRemaining > 0;
    }

    public boolean isActive() {
        return status == TeamInviteStatus.ACTIVE;
    }

    public void decrementUse() {
        if (usesRemaining > 0) {
            usesRemaining -= 1;
        }
        if (usesRemaining <= 0) {
            usesRemaining = 0;
            status = TeamInviteStatus.EXHAUSTED;
        }
    }

    public void markExpired() {
        status = TeamInviteStatus.EXPIRED;
    }

    public void markRevoked() {
        status = TeamInviteStatus.REVOKED;
    }

    public void markBlocked() {
        status = TeamInviteStatus.BLOCKED;
    }

    public void incrementFailedAttempts() {
        failedAttempts += 1;
    }

    public void resetFailedAttempts() {
        failedAttempts = 0;
    }
}

package com.passmais.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "moderation_logs")
public class ModerationLog {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID adminUserId;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(length = 2000)
    private String details;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @UpdateTimestamp
    private Instant updatedAt;
}

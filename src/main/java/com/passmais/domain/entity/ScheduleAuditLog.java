package com.passmais.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "schedule_audit_log")
public class ScheduleAuditLog {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private DoctorProfile doctor;

    @Column(name = "change_type", nullable = false, length = 20)
    private String changeType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_schedule", columnDefinition = "jsonb")
    private String oldSchedule;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_schedule", columnDefinition = "jsonb")
    private String newSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

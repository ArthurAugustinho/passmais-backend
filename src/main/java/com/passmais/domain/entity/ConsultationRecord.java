package com.passmais.domain.entity;

import com.passmais.domain.enums.ConsultationRecordStatus;
import jakarta.persistence.*;
import lombok.*;
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
@Table(name = "consultation_records")
public class ConsultationRecord {
    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ConsultationRecordStatus status = ConsultationRecordStatus.DRAFT;

    @Column(length = 500)
    private String reason;

    @Column(name = "symptom_duration", length = 120)
    private String symptomDuration;

    @Column(columnDefinition = "TEXT")
    private String anamnesis;

    @Column(name = "physical_exam", columnDefinition = "TEXT")
    private String physicalExam;

    @Column(columnDefinition = "TEXT")
    private String plan;

    @Column(name = "last_saved_at")
    private Instant lastSavedAt;

    @ManyToOne
    @JoinColumn(name = "updated_by_user_id")
    private User updatedBy;

    @Version
    private Long version;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}


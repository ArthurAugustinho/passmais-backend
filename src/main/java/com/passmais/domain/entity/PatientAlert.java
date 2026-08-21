package com.passmais.domain.entity;

import com.passmais.domain.enums.ClinicalAlertSeverity;
import com.passmais.domain.enums.ClinicalAlertType;
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
@Table(name = "patient_alerts")
public class PatientAlert {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientProfile patient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClinicalAlertType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClinicalAlertSeverity severity;

    @Column(nullable = false, length = 160)
    private String label;

    @Column(length = 500)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}


package com.passmais.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "doctor_secretaries")
public class DoctorSecretary {

    @EmbeddedId
    private DoctorSecretaryId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("doctorId")
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("secretaryId")
    @JoinColumn(name = "secretary_id", nullable = false)
    private User secretary;

    @Column(name = "linked_at")
    private Instant linkedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @PrePersist
    public void onPersist() {
        if (linkedAt == null) {
            linkedAt = Instant.now();
        }
    }
}

package com.passmais.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "doctor_schedule_specific_settings")
public class DoctorScheduleSpecificSettings {

    @Id
    private UUID doctorId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "doctor_id")
    private DoctorProfile doctor;

    @Column(name = "appointment_interval", nullable = false)
    private int appointmentInterval;

    @Column(name = "buffer_minutes", nullable = false)
    private int bufferMinutes;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

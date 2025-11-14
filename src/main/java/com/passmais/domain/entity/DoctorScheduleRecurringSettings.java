package com.passmais.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "doctor_schedule_recurring_settings")
public class DoctorScheduleRecurringSettings {

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

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "no_end_date", nullable = false)
    private boolean noEndDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "is_recurring_active", nullable = false)
    private boolean recurringActive;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

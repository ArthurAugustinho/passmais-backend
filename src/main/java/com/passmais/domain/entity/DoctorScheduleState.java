package com.passmais.domain.entity;

import com.passmais.domain.enums.ScheduleMode;
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
@Table(name = "doctor_schedule_state")
public class DoctorScheduleState {

    @Id
    private UUID doctorId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "doctor_id")
    private DoctorProfile doctor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleMode mode;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

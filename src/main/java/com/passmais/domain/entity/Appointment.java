package com.passmais.domain.entity;

import com.passmais.domain.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "appointments")
public class Appointment {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private DoctorProfile doctor;

    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne
    @JoinColumn(name = "dependent_id")
    private Dependent dependent;

    @Column(name = "date_time", nullable = false)
    private Instant dateTime;

    @Column(length = 500)
    private String observations;

    @Column(length = 500)
    private String reason;

    @Column(name = "symptom_duration", length = 120)
    private String symptomDuration;

    @Column(name = "pre_consult_notes", columnDefinition = "TEXT")
    private String preConsultNotes;

    @ManyToOne
    @JoinColumn(name = "rescheduled_from_id")
    private Appointment rescheduledFrom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.PENDING;

    private BigDecimal value;

    @Column(name = "booked_at")
    private Instant bookedAt;

    @Column(name = "patient_full_name", length = 160)
    private String patientFullName;

    @Column(name = "patient_cpf", length = 14)
    private String patientCpf;

    @Column(name = "patient_birth_date")
    private LocalDate patientBirthDate;

    @Column(name = "patient_cell_phone", length = 20)
    private String patientCellPhone;

    @Column(name = "location", length = 160)
    private String location;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    private Instant finalizedAt;

    @ManyToOne
    @JoinColumn(name = "updated_by_user_id")
    private User updatedBy;
}

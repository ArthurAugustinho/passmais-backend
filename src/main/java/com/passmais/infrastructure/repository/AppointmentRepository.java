package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.Appointment;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.PatientProfile;
import com.passmais.domain.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    List<Appointment> findByPatientAndDateTimeBetween(PatientProfile patient, Instant start, Instant end);
    long countByPatientAndDateTimeBetween(PatientProfile patient, Instant start, Instant end);
    boolean existsByDoctorAndDateTimeAndStatusIn(DoctorProfile doctor, Instant dateTime, List<AppointmentStatus> statuses);

    @Query("select count(a) from Appointment a where a.patient = :patient and a.rescheduledFrom is not null and a.dateTime between :start and :end")
    long countReschedulesInPeriod(@Param("patient") PatientProfile patient, @Param("start") Instant start, @Param("end") Instant end);
}


package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.Appointment;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.User;
import com.passmais.domain.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID>, JpaSpecificationExecutor<Appointment> {
    List<Appointment> findByPatientAndDateTimeBetween(User patient, Instant start, Instant end);
    long countByPatientAndDateTimeBetween(User patient, Instant start, Instant end);
    boolean existsByDoctorAndDateTimeAndStatusIn(DoctorProfile doctor, Instant dateTime, List<AppointmentStatus> statuses);
    boolean existsByDoctorAndPatientAndStatusInAndDateTimeGreaterThanEqual(DoctorProfile doctor,
                                                                           User patient,
                                                                           List<AppointmentStatus> statuses,
                                                                           Instant dateTime);

    @Query("select count(a) from Appointment a where a.patient = :patient and a.rescheduledFrom is not null and a.dateTime between :start and :end")
    long countReschedulesInPeriod(@Param("patient") User patient, @Param("start") Instant start, @Param("end") Instant end);

    List<Appointment> findByDateTimeBetween(Instant start, Instant end);
    List<Appointment> findByDoctorAndStatus(DoctorProfile doctor, AppointmentStatus status);
    List<Appointment> findByDoctor(DoctorProfile doctor);
    List<Appointment> findByPatientAndStatus(User patient, AppointmentStatus status);
    List<Appointment> findByPatient(User patient);

    @Query("select a from Appointment a where a.doctor = :doctor and a.dateTime between :start and :end")
    List<Appointment> findByDoctorAndDateTimeBetween(@Param("doctor") DoctorProfile doctor, @Param("start") Instant start, @Param("end") Instant end);
}

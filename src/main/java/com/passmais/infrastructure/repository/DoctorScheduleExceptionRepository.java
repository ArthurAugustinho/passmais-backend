package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.DoctorScheduleException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DoctorScheduleExceptionRepository extends JpaRepository<DoctorScheduleException, UUID> {
    List<DoctorScheduleException> findByDoctor(DoctorProfile doctor);
    boolean existsByDoctorAndExceptionDate(DoctorProfile doctor, LocalDate exceptionDate);
    void deleteByDoctor(DoctorProfile doctor);
    void deleteByDoctorAndExceptionDate(DoctorProfile doctor, LocalDate exceptionDate);
}

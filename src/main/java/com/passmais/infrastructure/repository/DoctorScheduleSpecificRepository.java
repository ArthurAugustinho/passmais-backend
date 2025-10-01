package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.DoctorScheduleSpecific;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorScheduleSpecificRepository extends JpaRepository<DoctorScheduleSpecific, UUID> {
    List<DoctorScheduleSpecific> findByDoctorOrderByDateAsc(DoctorProfile doctor);
    Optional<DoctorScheduleSpecific> findByDoctorAndDate(DoctorProfile doctor, LocalDate date);
    void deleteByDoctor(DoctorProfile doctor);
}

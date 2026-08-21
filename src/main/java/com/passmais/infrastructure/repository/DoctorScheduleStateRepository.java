package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.DoctorScheduleState;
import com.passmais.domain.enums.ScheduleMode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DoctorScheduleStateRepository extends JpaRepository<DoctorScheduleState, UUID> {
    Optional<DoctorScheduleState> findByDoctor(DoctorProfile doctor);
    boolean existsByDoctorAndMode(DoctorProfile doctor, ScheduleMode mode);
}

package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.DoctorScheduleSpecificSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DoctorScheduleSpecificSettingsRepository extends JpaRepository<DoctorScheduleSpecificSettings, UUID> {
}

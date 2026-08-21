package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.DoctorScheduleRecurringSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DoctorScheduleRecurringSettingsRepository extends JpaRepository<DoctorScheduleRecurringSettings, UUID> {
}

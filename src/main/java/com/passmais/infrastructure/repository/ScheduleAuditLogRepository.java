package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.ScheduleAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ScheduleAuditLogRepository extends JpaRepository<ScheduleAuditLog, UUID> {
}

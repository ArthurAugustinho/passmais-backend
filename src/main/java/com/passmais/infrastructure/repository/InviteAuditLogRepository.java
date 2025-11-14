package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.InviteAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InviteAuditLogRepository extends JpaRepository<InviteAuditLog, UUID> {
}

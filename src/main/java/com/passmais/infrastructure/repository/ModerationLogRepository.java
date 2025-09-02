package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.ModerationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ModerationLogRepository extends JpaRepository<ModerationLog, UUID> {}


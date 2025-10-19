package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.ConsultationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConsultationRecordRepository extends JpaRepository<ConsultationRecord, UUID> {
    Optional<ConsultationRecord> findByAppointmentId(UUID appointmentId);
}


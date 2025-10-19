package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface PatientProfileRepository extends JpaRepository<PatientProfile, UUID> {
    boolean existsByCellPhone(String cellPhone);
    Optional<PatientProfile> findByUserId(UUID userId);
    java.util.List<PatientProfile> findByUserIdIn(Collection<UUID> userIds);
}

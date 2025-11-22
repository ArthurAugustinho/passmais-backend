package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.DoctorSecretary;
import com.passmais.domain.entity.DoctorSecretaryId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorSecretaryRepository extends JpaRepository<DoctorSecretary, DoctorSecretaryId> {

    @EntityGraph(attributePaths = {"secretary"})
    List<DoctorSecretary> findAllByIdDoctorIdAndActiveTrue(UUID doctorId);

    @EntityGraph(attributePaths = {"doctor"})
    List<DoctorSecretary> findAllByIdSecretaryIdAndActiveTrue(UUID secretaryId);

    Optional<DoctorSecretary> findByIdDoctorIdAndIdSecretaryId(UUID doctorId, UUID secretaryId);
}

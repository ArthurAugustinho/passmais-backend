package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.PatientFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientFileRepository extends JpaRepository<PatientFile, UUID> {
    Optional<PatientFile> findByCpf(String cpf);
    List<PatientFile> findByCpfIn(Collection<String> cpfs);
}

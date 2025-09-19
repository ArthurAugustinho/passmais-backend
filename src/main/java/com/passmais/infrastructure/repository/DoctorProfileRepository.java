package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, UUID> {
    List<DoctorProfile> findBySpecialtyContainingIgnoreCase(String specialty);
    boolean existsByCpf(String cpf);
    boolean existsByCrm(String crm);
    boolean existsByPhone(String phone);
    List<DoctorProfile> findByApprovedAtIsNullAndRejectedAtIsNull();
}

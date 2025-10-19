package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.DoctorProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, UUID> {
    List<DoctorProfile> findBySpecialtyContainingIgnoreCase(String specialty);
    boolean existsByCpf(String cpf);
    boolean existsByCrm(String crm);
    boolean existsByPhone(String phone);
    List<DoctorProfile> findByApprovedAtIsNullAndRejectedAtIsNull();
    Optional<DoctorProfile> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DoctorProfile d where d.id = :doctorId")
    Optional<DoctorProfile> findByIdForUpdate(@Param("doctorId") UUID doctorId);
}

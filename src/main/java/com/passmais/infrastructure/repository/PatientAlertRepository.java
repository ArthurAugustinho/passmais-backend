package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.PatientAlert;
import com.passmais.domain.entity.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PatientAlertRepository extends JpaRepository<PatientAlert, UUID> {
    List<PatientAlert> findByPatientAndActiveIsTrue(PatientProfile patient);
    List<PatientAlert> findByPatientIdAndActiveIsTrue(UUID patientId);
    List<PatientAlert> findByPatientIdInAndActiveIsTrue(Collection<UUID> patientIds);

    @Query("select pa from PatientAlert pa where pa.patient.user.id = :userId and pa.active = true")
    List<PatientAlert> findByPatientUserIdAndActiveIsTrue(@Param("userId") UUID patientUserId);

    @Query("select pa from PatientAlert pa where pa.patient.user.id in :userIds and pa.active = true")
    List<PatientAlert> findByPatientUserIdInAndActiveIsTrue(@Param("userIds") Collection<UUID> patientUserIds);
}

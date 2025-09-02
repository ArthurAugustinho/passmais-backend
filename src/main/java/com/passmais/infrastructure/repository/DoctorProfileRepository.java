package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, UUID> {}


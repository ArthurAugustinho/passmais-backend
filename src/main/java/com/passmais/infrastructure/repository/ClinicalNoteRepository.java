package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.ClinicalNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, UUID> {}


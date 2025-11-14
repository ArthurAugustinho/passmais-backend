package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.TeamInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;

public interface TeamInviteRepository extends JpaRepository<TeamInvite, UUID> {

    Optional<TeamInvite> findByInviteCodeHash(String inviteCodeHash);

    boolean existsByInviteCodeHash(String inviteCodeHash);

    List<TeamInvite> findAllByDoctorId(UUID doctorId);

    Optional<TeamInvite> findByDoctorIdAndInviteCodeHash(UUID doctorId, String inviteCodeHash);

    @EntityGraph(attributePaths = {"doctor"})
    List<TeamInvite> findAllBySecretaryCorporateEmailIgnoreCase(String secretaryCorporateEmail);
}

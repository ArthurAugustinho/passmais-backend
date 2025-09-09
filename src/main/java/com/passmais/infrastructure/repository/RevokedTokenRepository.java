package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, UUID> {
    Optional<RevokedToken> findFirstByTokenAndExpiresAtAfter(String token, Instant now);
}


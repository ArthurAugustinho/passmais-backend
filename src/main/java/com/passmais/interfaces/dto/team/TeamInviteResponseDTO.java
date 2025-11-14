package com.passmais.interfaces.dto.team;

import java.time.Instant;

public record TeamInviteResponseDTO(
        String code,
        String status,
        int usesRemaining,
        Instant expiresAt
) {}

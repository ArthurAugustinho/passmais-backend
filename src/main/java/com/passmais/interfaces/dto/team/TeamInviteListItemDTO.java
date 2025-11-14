package com.passmais.interfaces.dto.team;

import java.time.Instant;

public record TeamInviteListItemDTO(
        String code,
        String status,
        int usesRemaining,
        Instant expiresAt,
        String secretaryFullName,
        String secretaryCorporateEmail
) {}

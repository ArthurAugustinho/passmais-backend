package com.passmais.interfaces.dto.team;

import java.time.Instant;
import java.util.UUID;

public record TeamSecretaryResponseDTO(
        UUID id,
        String name,
        String email,
        Instant linkedAt
) {}

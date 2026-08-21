package com.passmais.interfaces.dto.consultation;

import java.time.Instant;

public record ConsultationFinalizeResponse(
        String status,
        Instant finalizedAt
) {}

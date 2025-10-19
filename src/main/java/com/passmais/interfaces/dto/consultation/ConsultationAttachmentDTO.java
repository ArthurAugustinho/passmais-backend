package com.passmais.interfaces.dto.consultation;

import java.time.Instant;
import java.util.UUID;

public record ConsultationAttachmentDTO(
        UUID id,
        String name,
        String url,
        Instant uploadedAt
) {}


package com.passmais.interfaces.dto.consultation;

import java.time.Instant;

public record ConsultationRecordPatchResponse(
        String status,
        Instant lastSavedAt
) {}

package com.passmais.interfaces.dto.consultation;

import java.time.Instant;
import java.util.UUID;

public record ConsultationRecordDTO(
        UUID id,
        String status,
        String reason,
        String symptomDuration,
        String anamnesis,
        String physicalExam,
        String plan,
        Instant lastSavedAt
) {}


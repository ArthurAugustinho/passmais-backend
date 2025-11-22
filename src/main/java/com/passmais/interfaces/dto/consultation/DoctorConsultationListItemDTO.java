package com.passmais.interfaces.dto.consultation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DoctorConsultationListItemDTO(
        UUID id,
        Instant scheduledAt,
        String status,
        String reason,
        String symptomDuration,
        List<ClinicalAlertDTO> alerts,
        PatientSummaryDTO patient
) {}


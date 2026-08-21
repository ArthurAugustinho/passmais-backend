package com.passmais.interfaces.dto.consultation;

import java.time.LocalDate;
import java.util.UUID;

public record ConsultationMedicationDTO(
        UUID id,
        String name,
        String dose,
        String schedule,
        String adherence,
        LocalDate updatedAt
) {}

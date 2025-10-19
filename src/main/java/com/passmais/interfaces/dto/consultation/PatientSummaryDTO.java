package com.passmais.interfaces.dto.consultation;

import java.time.LocalDate;
import java.util.UUID;

public record PatientSummaryDTO(
        UUID id,
        String name,
        LocalDate birthDate,
        String gender,
        String avatarUrl,
        boolean emailMasked,
        boolean addressMasked
) {}


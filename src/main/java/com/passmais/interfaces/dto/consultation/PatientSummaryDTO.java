package com.passmais.interfaces.dto.consultation;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PatientSummaryDTO(
        UUID id,
        String name,
        LocalDate birthDate,
        String gender,
        String avatarUrl,
        String cpf,
        Instant presenceConfirmedAt,
        boolean emailMasked,
        boolean addressMasked
) {}

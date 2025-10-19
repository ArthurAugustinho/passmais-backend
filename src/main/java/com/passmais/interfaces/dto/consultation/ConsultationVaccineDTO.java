package com.passmais.interfaces.dto.consultation;

import java.time.LocalDate;
import java.util.UUID;

public record ConsultationVaccineDTO(
        UUID id,
        String name,
        LocalDate date,
        String status
) {}

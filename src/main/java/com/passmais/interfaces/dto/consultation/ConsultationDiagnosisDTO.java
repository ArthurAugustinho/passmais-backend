package com.passmais.interfaces.dto.consultation;

import java.util.UUID;

public record ConsultationDiagnosisDTO(
        UUID id,
        String code,
        String description
) {}

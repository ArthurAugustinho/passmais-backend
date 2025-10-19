package com.passmais.interfaces.dto.consultation;

import java.util.UUID;

public record ClinicalAlertDTO(
        UUID id,
        String type,
        String label,
        String severity
) {}


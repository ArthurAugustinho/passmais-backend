package com.passmais.interfaces.dto;

import java.util.UUID;

public record PatientFileUpsertResponseDTO(
        UUID id,
        String message
) {}

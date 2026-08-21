package com.passmais.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ClinicalNoteCreateDTO(
        @NotNull UUID appointmentId,
        @NotNull UUID doctorId,
        @NotBlank String notes
) {}


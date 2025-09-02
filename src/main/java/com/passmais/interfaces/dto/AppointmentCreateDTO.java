package com.passmais.interfaces.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record AppointmentCreateDTO(
        @NotNull(message = "ID do médico é obrigatório") UUID doctorId,
        @NotNull(message = "ID do paciente é obrigatório") UUID patientId,
        UUID dependentId,
        @NotNull(message = "Data/hora é obrigatória") Instant dateTime
) {}


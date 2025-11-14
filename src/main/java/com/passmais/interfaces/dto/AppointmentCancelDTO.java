package com.passmais.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record AppointmentCancelDTO(
        @NotBlank(message = "Motivo é obrigatório") String reason
) {}


package com.passmais.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record AppointmentObservationDTO(
        @NotBlank(message = "Observação é obrigatória") String text
) {}


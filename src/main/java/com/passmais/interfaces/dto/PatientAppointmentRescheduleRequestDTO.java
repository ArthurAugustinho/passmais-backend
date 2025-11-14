package com.passmais.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record PatientAppointmentRescheduleRequestDTO(
        @NotBlank(message = "Nova data é obrigatória") String newDate,
        @NotBlank(message = "Novo horário é obrigatório") String newTime
) {}

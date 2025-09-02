package com.passmais.interfaces.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record AvailabilityCreateDTO(
        @NotNull(message = "Dia da semana é obrigatório") DayOfWeek dayOfWeek,
        @NotNull(message = "Hora inicial é obrigatória") LocalTime startTime,
        @NotNull(message = "Hora final é obrigatória") LocalTime endTime
) {}


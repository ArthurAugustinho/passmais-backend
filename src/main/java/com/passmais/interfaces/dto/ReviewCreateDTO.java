package com.passmais.interfaces.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReviewCreateDTO(
        @NotNull(message = "ID da consulta é obrigatório") UUID appointmentId,
        @Min(value = 1, message = "Nota mínima é 1") @Max(value = 5, message = "Nota máxima é 5") int rating,
        @NotBlank(message = "Comentário é obrigatório") String comment
) {}


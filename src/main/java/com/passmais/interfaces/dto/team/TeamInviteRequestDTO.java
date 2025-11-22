package com.passmais.interfaces.dto.team;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record TeamInviteRequestDTO(
        @NotNull @Positive @Max(1) Integer maxUses,
        @NotNull @Future Instant expiresAt,
        @NotBlank(message = "Nome completo da secretária é obrigatório") String secretaryFullName,
        @NotBlank(message = "E-mail corporativo da secretária é obrigatório")
        @Email(message = "E-mail corporativo inválido") String secretaryCorporateEmail
) {}

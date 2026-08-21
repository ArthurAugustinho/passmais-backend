package com.passmais.interfaces.dto.team;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamJoinRequestDTO(
        @NotBlank(message = "Código de convite é obrigatório") String inviteCode,
        @Email(message = "E-mail inválido") @NotBlank(message = "E-mail é obrigatório") String email,
        @NotBlank(message = "Senha é obrigatória") @Size(min = 8, message = "Senha deve ter ao menos 8 caracteres") String password,
        @NotBlank(message = "Nome é obrigatório") String name
) {}

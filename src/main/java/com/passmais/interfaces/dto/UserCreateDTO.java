package com.passmais.interfaces.dto;

import com.passmais.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateDTO(
        @NotBlank(message = "Nome é obrigatório") @Size(max = 120, message = "Nome muito longo") String name,
        @Email(message = "E-mail inválido") @NotBlank(message = "E-mail é obrigatório") String email,
        @NotBlank(message = "Senha é obrigatória") @Size(min = 6, message = "Senha deve ter ao menos 6 caracteres") String password,
        @NotNull(message = "Perfil é obrigatório") Role role,
        @NotNull(message = "Aceite da LGPD é obrigatório") Boolean lgpdAccepted
) {}


package com.passmais.interfaces.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequestDTO(
        @Email(message = "E-mail inválido") @NotBlank(message = "E-mail é obrigatório") String email,
        @NotBlank(message = "Senha é obrigatória") @Size(min = 6, message = "Senha deve possuir 6 caracteres ou mais") String password
) {}

package com.passmais.interfaces.dto;

import com.passmais.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminUserCreateDTO(
        @NotBlank String name,
        @Email @NotBlank String email,
        @NotBlank String password,
        @NotNull Role role
) {}


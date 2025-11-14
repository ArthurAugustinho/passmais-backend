package com.passmais.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record PatientFileResponsibleDTO(
        @NotBlank String fullName,
        @NotBlank String relationship,
        @NotBlank String cpf,
        @NotBlank String phone
) {}

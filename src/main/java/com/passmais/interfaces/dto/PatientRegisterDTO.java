package com.passmais.interfaces.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record PatientRegisterDTO(
        @NotBlank String name,
        @Email @NotBlank String email,
        @NotBlank @Size(min = 6) String password,
        @NotNull Boolean lgpdAccepted,

        @NotBlank String cpf,
        LocalDate birthDate,
        @Size(max = 255) String address,
        @NotBlank String cellPhone,
        @Size(max = 30) String communicationPreference
) {}


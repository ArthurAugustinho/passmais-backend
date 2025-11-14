package com.passmais.interfaces.dto;

import com.passmais.domain.enums.PatientSex;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;

public record PatientFileUpsertRequestDTO(
        @NotBlank String fullName,
        @NotBlank String cpf,
        @NotNull LocalDate birthDate,
        String motherName,
        PatientSex sex,
        @Email String email,
        @NotBlank String contactPhone,
        String fullAddress,
        @NotNull Boolean hasLegalResponsible,
        @Valid PatientFileResponsibleDTO responsible,
        @Valid PatientFileHealthInsuranceDTO healthInsurance,
        Instant presenceConfirmedAt
) {}

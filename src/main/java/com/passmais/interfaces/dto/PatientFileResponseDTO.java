package com.passmais.interfaces.dto;

import com.passmais.domain.enums.PatientSex;

import java.time.Instant;

public record PatientFileResponseDTO(
        String fullName,
        String cpf,
        String motherName,
        PatientSex sex,
        String email,
        String contactPhone,
        String fullAddress,
        Boolean hasLegalResponsible,
        PatientFileResponsibleDTO responsible,
        PatientFileHealthInsuranceDTO healthInsurance,
        Instant presenceConfirmedAt
) {}

package com.passmais.interfaces.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.passmais.interfaces.json.StrictBooleanDeserializer;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DoctorRegisterDTO(
        @NotBlank String name,
        @Email @NotBlank String email,
        @NotBlank @Size(min = 6) String password,
        @NotBlank @Size(min = 6) String confirmPassword,
        @NotNull @JsonDeserialize(using = StrictBooleanDeserializer.class) Boolean lgpdAccepted,

        @NotBlank String crm,
        @NotBlank String phone,
        @NotBlank String cpf,
        @NotNull LocalDate birthDate,
        @Size(max = 500) String bio,
        @Size(max = 80) String specialty,
        @Positive @Digits(integer = 10, fraction = 2) BigDecimal consultationPrice,
        @NotBlank String clinicName,
        @NotBlank String streetAndNumber,
        @NotBlank String city,
        @NotBlank String postalCode,
        String photoUrl
) {}

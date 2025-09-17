package com.passmais.interfaces.dto;

import com.passmais.domain.enums.Role;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DoctorProfileAdminDTO(
        UUID id,
        String userName,
        String userEmail,
        Role userRole,
        String crm,
        String specialty,
        String bio,
        String phone,
        String cpf,
        LocalDate birthDate,
        String photoUrl,
        BigDecimal consultationPrice,
        boolean approved,
        Instant approvedAt,
        Instant createdAt,
        Instant updatedAt
) {}

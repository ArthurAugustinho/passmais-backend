package com.passmais.interfaces.dto;

import com.passmais.domain.enums.AppointmentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AppointmentResponseDTO(
        UUID id,
        UUID doctorId,
        UUID patientId,
        Instant dateTime,
        Instant bookedAt,
        BigDecimal value,
        String location,
        String patientFullName,
        String patientCpf,
        LocalDate patientBirthDate,
        String patientCellPhone,
        String reason,
        AppointmentStatus status
) {}

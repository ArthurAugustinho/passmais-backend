package com.passmais.interfaces.dto;

import com.passmais.domain.enums.AppointmentStatus;

import java.time.Instant;
import java.util.UUID;

public record AppointmentResponseDTO(
        UUID id,
        UUID doctorId,
        UUID patientId,
        Instant dateTime,
        AppointmentStatus status
) {}


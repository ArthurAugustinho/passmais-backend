package com.passmais.interfaces.dto;

import java.math.BigDecimal;

import java.util.UUID;

public record PatientAppointmentListItemDTO(
        UUID id,
        String date,
        String time,
        String doctorName,
        String patientName,
        String clinicAddress,
        BigDecimal price,
        String status
) {}

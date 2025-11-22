package com.passmais.interfaces.dto;

public record AppointmentConfirmationResponseDTO(
        String message,
        AppointmentResponseDTO appointment
) {}

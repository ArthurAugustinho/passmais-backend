package com.passmais.interfaces.dto;

public record PatientPresenceResponseDTO(
        PatientFileResponseDTO patientFile,
        AppointmentResponseDTO appointment
) {}

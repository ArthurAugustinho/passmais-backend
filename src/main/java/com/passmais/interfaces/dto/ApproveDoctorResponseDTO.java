package com.passmais.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record ApproveDoctorResponseDTO(
        String message,
        Doctor doctor
) {
    public record Doctor(
            UUID id,
            @JsonProperty("approved_at") Instant approvedAt
    ) {}
}


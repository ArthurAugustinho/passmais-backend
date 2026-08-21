package com.passmais.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record ApproveClinicResponseDTO(
        String message,
        Clinic clinic
) {
    public record Clinic(
            UUID id,
            @JsonProperty("approved_at") Instant approvedAt
    ) {}
}


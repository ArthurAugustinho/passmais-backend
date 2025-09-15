package com.passmais.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record RejectClinicResponseDTO(
        String message,
        Clinic clinic
) {
    public record Clinic(
            UUID id,
            @JsonProperty("rejected_at") Instant rejectedAt
    ) {}
}


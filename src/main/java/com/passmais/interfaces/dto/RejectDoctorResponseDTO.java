package com.passmais.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record RejectDoctorResponseDTO(
        String message,
        Doctor doctor
) {
    public record Doctor(
            UUID id,
            @JsonProperty("rejected_at") Instant rejectedAt,
            @JsonProperty("failure_description") String failureDescription
    ) {}
}

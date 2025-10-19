package com.passmais.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record PendingDoctorDTO(
        UUID id,
        String name,
        String specialty,
        String crm,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("approved_at") Instant approvedAt
) {}

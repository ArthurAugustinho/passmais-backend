package com.passmais.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record PendingClinicDTO(
        UUID id,
        String name,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("approved_at") Instant approvedAt
) {}


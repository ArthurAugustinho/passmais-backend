package com.passmais.interfaces.dto.team;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TeamLinkDeleteRequestDTO(
        @NotNull UUID doctorId,
        @NotNull UUID secretaryId
) {}

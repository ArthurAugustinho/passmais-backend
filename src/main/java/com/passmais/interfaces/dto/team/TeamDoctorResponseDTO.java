package com.passmais.interfaces.dto.team;

import java.util.UUID;

public record TeamDoctorResponseDTO(
        UUID id,
        String name,
        String specialty
) {}

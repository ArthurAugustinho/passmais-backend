package com.passmais.interfaces.dto.team;

import com.passmais.domain.enums.Role;

import java.util.UUID;

public record TeamJoinResponseDTO(
        String message,
        Role role,
        LinkedDoctorDTO linkedDoctor
) {
    public record LinkedDoctorDTO(UUID id, String name) {}
}

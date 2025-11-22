package com.passmais.interfaces.dto;

import com.passmais.domain.enums.Role;

public record LoginResponseDTO(
        String accessToken,
        String fullName,
        Role role
) {}


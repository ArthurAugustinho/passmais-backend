package com.passmais.interfaces.dto;

import com.passmais.domain.enums.Role;

import java.util.UUID;

public record UserResponseDTO(UUID id, String name, String email, Role role) {}


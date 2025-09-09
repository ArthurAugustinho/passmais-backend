package com.passmais.interfaces.dto;

import java.util.UUID;

public record DoctorPublicProfileDTO(
        UUID id,
        String name,
        String crm,
        String specialty,
        String bio,
        Double averageRating,
        long reviewsCount
) {}


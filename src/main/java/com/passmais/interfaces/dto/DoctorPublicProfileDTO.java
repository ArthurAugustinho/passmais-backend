package com.passmais.interfaces.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DoctorPublicProfileDTO(
        UUID id,
        String name,
        String crm,
        String specialty,
        String bio,
        String photoUrl,
        BigDecimal consultationPrice,
        Double averageRating,
        long reviewsCount
) {}

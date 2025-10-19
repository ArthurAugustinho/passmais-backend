package com.passmais.interfaces.dto.consultation;

import java.time.LocalDate;
import java.util.UUID;

public record ConsultationExamDTO(
        UUID id,
        String kind,
        String exam,
        LocalDate date,
        boolean abnormal,
        String highlight
) {}

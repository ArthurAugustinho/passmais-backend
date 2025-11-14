package com.passmais.interfaces.dto.consultation;

public record ConsultationFinalizeRequest(
        String reason,
        String symptomDuration,
        String anamnesis,
        String physicalExam,
        String plan
) {}

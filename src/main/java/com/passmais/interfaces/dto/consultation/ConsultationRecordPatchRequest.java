package com.passmais.interfaces.dto.consultation;

public record ConsultationRecordPatchRequest(
        String reason,
        String symptomDuration,
        String anamnesis,
        String physicalExam,
        String plan,
        String status
) {}

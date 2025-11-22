package com.passmais.interfaces.dto.consultation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DoctorConsultationDetailDTO(
        UUID id,
        Instant scheduledAt,
        String status,
        String reason,
        String symptomDuration,
        String preConsultNotes,
        ConsultationRecordDTO consultationRecord,
        List<ClinicalAlertDTO> alerts,
        List<ConsultationMedicationDTO> medications,
        List<ConsultationExamDTO> exams,
        List<ConsultationVaccineDTO> vaccines,
        List<ConsultationDiagnosisDTO> diagnoses,
        List<ConsultationAttachmentDTO> attachments,
        PatientSummaryDTO patient
) {}

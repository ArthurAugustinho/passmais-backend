package com.passmais.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.passmais.domain.enums.AppointmentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SecretaryAppointmentResponseDTO(
        @JsonProperty("doctor_full_name") String doctorFullName,
        @JsonProperty("doctor_crm") String doctorCrm,
        @JsonProperty("dependent_id") UUID dependentId,
        @JsonProperty("date_time") Instant dateTime,
        String observations,
        String reason,
        @JsonProperty("symptom_duration") String symptomDuration,
        @JsonProperty("pre_consult_notes") String preConsultNotes,
        @JsonProperty("rescheduled_from_id") UUID rescheduledFromId,
        AppointmentStatus status,
        BigDecimal value,
        @JsonProperty("booked_at") Instant bookedAt,
        @JsonProperty("patient_full_name") String patientFullName,
        @JsonProperty("patient_cpf") String patientCpf,
        @JsonProperty("patient_birth_date") LocalDate patientBirthDate,
        @JsonProperty("patient_cell_phone") String patientCellPhone,
        String location,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("finalized_at") Instant finalizedAt
) {}

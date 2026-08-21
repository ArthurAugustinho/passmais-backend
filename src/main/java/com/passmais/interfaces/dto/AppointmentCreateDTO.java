package com.passmais.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AppointmentCreateDTO(
        @NotNull(message = "ID do médico é obrigatório") UUID doctorId,
        @NotNull(message = "ID do paciente é obrigatório") UUID patientId,
        @NotNull(message = "Data e hora da consulta são obrigatórias") Instant appointmentDateTime,
        @NotNull(message = "Hora do agendamento é obrigatória") Instant bookingDateTime,
        @NotNull(message = "Valor da consulta é obrigatório") BigDecimal consultationValue,
        @NotBlank(message = "Número de celular do paciente é obrigatório") String patientCellPhone,
        @NotBlank(message = "Nome completo do paciente é obrigatório") String patientFullName,
        @NotBlank(message = "CPF do paciente é obrigatório") String patientCpf,
        @NotNull(message = "Data de nascimento do paciente é obrigatória") LocalDate patientBirthDate,
        @NotBlank(message = "Local de atendimento é obrigatório") String location,
        String reason
) {}

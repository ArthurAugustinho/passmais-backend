package com.passmais.application.service.consultation;

import com.passmais.domain.enums.AppointmentStatus;
import com.passmais.domain.enums.ConsultationRecordStatus;
import com.passmais.domain.exception.ApiErrorException;
import org.springframework.http.HttpStatus;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class ConsultationStatusMapper {

    private static final Map<String, AppointmentStatus> APPOINTMENT_STATUS_BY_API = Map.of(
            "agendada", AppointmentStatus.PENDING,
            "confirmada", AppointmentStatus.CONFIRMED,
            "em-andamento", AppointmentStatus.IN_PROGRESS,
            "concluida", AppointmentStatus.DONE,
            "cancelada", AppointmentStatus.CANCELED
    );

    private ConsultationStatusMapper() {
    }

    public static AppointmentStatus fromApiAppointmentStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new ApiErrorException("VALIDATION_ERROR", "Status não pode ser vazio", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        AppointmentStatus mapped = APPOINTMENT_STATUS_BY_API.get(status.toLowerCase(Locale.ROOT));
        if (mapped == null) {
            throw new ApiErrorException("VALIDATION_ERROR", "Status inválido: " + status, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return mapped;
    }

    public static String toApiAppointmentStatus(AppointmentStatus status) {
        return switch (Objects.requireNonNull(status)) {
            case PENDING -> "agendada";
            case CONFIRMED -> "confirmada";
            case IN_PROGRESS -> "em-andamento";
            case DONE -> "concluida";
            case CANCELED -> "cancelada";
        };
    }

    public static String toApiConsultationRecordStatus(ConsultationRecordStatus status) {
        return switch (Objects.requireNonNull(status)) {
            case DRAFT -> "draft";
            case FINALIZED -> "finalized";
        };
    }

    public static ConsultationRecordStatus fromApiConsultationRecordStatus(String status) {
        if (status == null) {
            return null;
        }
        return switch (status.toLowerCase(Locale.ROOT)) {
            case "draft" -> ConsultationRecordStatus.DRAFT;
            case "finalized" -> ConsultationRecordStatus.FINALIZED;
            default -> throw new ApiErrorException("VALIDATION_ERROR", "Status inválido para prontuário: " + status, HttpStatus.UNPROCESSABLE_ENTITY);
        };
    }
}


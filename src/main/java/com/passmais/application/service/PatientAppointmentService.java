package com.passmais.application.service;

import com.passmais.domain.entity.Appointment;
import com.passmais.domain.entity.User;
import com.passmais.domain.enums.AppointmentStatus;
import com.passmais.infrastructure.repository.AppointmentRepository;
import com.passmais.interfaces.dto.PatientAppointmentListItemDTO;
import org.springframework.util.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
public class PatientAppointmentService {

    private static final EnumSet<AppointmentStatus> PATIENT_CANCELABLE_STATUSES =
            EnumSet.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);
    private static final EnumSet<AppointmentStatus> PATIENT_RESCHEDULABLE_STATUSES =
            EnumSet.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);
    private static final List<AppointmentStatus> DOCTOR_BUSY_STATUSES = List.of(
            AppointmentStatus.PENDING,
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.IN_PROGRESS,
            AppointmentStatus.DONE
    );
    private static final ZoneId SYSTEM_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentRepository appointmentRepository;
    private final AppointmentService appointmentService;

    public PatientAppointmentService(AppointmentRepository appointmentRepository,
                                     AppointmentService appointmentService) {
        this.appointmentRepository = appointmentRepository;
        this.appointmentService = appointmentService;
    }

    public List<PatientAppointmentListItemDTO> listAppointments(User patient) {
        return appointmentRepository.findByPatient(patient).stream()
                .sorted(Comparator.comparing(Appointment::getDateTime))
                .map(this::toListItem)
                .toList();
    }

    public Appointment cancelAppointment(User patient, UUID appointmentId, String reason) {
        Appointment appointment = loadOwnedAppointment(patient, appointmentId);
        if (!PATIENT_CANCELABLE_STATUSES.contains(appointment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Consulta não pode ser cancelada.");
        }
        return appointmentService.cancel(appointment, reason);
    }

    public Appointment rescheduleAppointment(User patient, UUID appointmentId, Instant newDateTime) {
        Appointment appointment = loadOwnedAppointment(patient, appointmentId);
        if (!PATIENT_RESCHEDULABLE_STATUSES.contains(appointment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Consulta não pode ser reagendada.");
        }
        Instant now = Instant.now();
        if (newDateTime.isBefore(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nova data deve ser no futuro.");
        }
        if (!newDateTime.equals(appointment.getDateTime()) &&
                appointmentRepository.existsByDoctorAndDateTimeAndStatusIn(
                        appointment.getDoctor(),
                        newDateTime,
                        DOCTOR_BUSY_STATUSES
                )) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Médico indisponível para o novo horário.");
        }
        appointment.setDateTime(newDateTime);
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setCanceledAt(null);
        appointment.setCanceledReason(null);
        return appointmentRepository.save(appointment);
    }

    private Appointment loadOwnedAppointment(User patient, UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consulta não encontrada."));
        if (appointment.getPatient() == null || !appointment.getPatient().getId().equals(patient.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Consulta não pertence ao paciente autenticado.");
        }
        return appointment;
    }

    private PatientAppointmentListItemDTO toListItem(Appointment appointment) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(appointment.getDateTime(), SYSTEM_ZONE);
        UUID doctorId = appointment.getDoctor() != null ? appointment.getDoctor().getId() : null;
        String doctorName = appointment.getDoctor() != null && appointment.getDoctor().getUser() != null
                ? appointment.getDoctor().getUser().getName()
                : null;
        String patientName = resolvePatientName(appointment);
        String clinicAddress = appointment.getDoctor() != null
                ? appointment.getDoctor().getClinicStreetAndNumber()
                : null;
        return new PatientAppointmentListItemDTO(
                appointment.getId(),
                doctorId,
                DATE_FORMAT.format(localDateTime),
                TIME_FORMAT.format(localDateTime),
                doctorName,
                patientName,
                clinicAddress,
                appointment.getValue(),
                resolveStatus(appointment.getStatus())
        );
    }

    private String resolvePatientName(Appointment appointment) {
        if (appointment == null) {
            return null;
        }
        if (StringUtils.hasText(appointment.getPatientFullName())) {
            return appointment.getPatientFullName();
        }
        if (appointment.getPatient() != null) {
            return appointment.getPatient().getName();
        }
        return null;
    }

    private String resolveStatus(AppointmentStatus status) {
        if (status == null) {
            return "agendado";
        }
        return switch (status) {
            case DONE -> "realizado";
            case CANCELED -> "cancelado";
            default -> "agendado";
        };
    }
}

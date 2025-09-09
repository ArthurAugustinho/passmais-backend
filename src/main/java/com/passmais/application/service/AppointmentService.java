package com.passmais.application.service;

import com.passmais.domain.entity.Appointment;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.PatientProfile;
import com.passmais.domain.enums.AppointmentStatus;
import com.passmais.infrastructure.repository.AppointmentRepository;
import com.passmais.infrastructure.repository.NotificationRepository;
import com.passmais.domain.entity.Notification;
import com.passmais.domain.enums.NotificationType;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final NotificationRepository notificationRepository;
    private static final int MAX_RESCHEDULES_IN_30_DAYS = 2;
    private static final Duration CANCEL_MIN_NOTICE = Duration.ofHours(6); // antecedência mínima de cancelamento: 6h

    public AppointmentService(AppointmentRepository appointmentRepository, NotificationRepository notificationRepository) {
        this.appointmentRepository = appointmentRepository;
        this.notificationRepository = notificationRepository;
    }

    public Appointment schedule(DoctorProfile doctor, PatientProfile patient, Instant dateTime) {
        if (dateTime.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Consulta deve ser no futuro");
        }
        // checar conflito do médico em status relevantes
        if (appointmentRepository.existsByDoctorAndDateTimeAndStatusIn(doctor, dateTime,
                List.of(AppointmentStatus.PENDING, AppointmentStatus.IN_PROGRESS, AppointmentStatus.DONE))) {
            throw new IllegalArgumentException("Médico indisponível neste horário");
        }
        Appointment appt = Appointment.builder()
                .doctor(doctor)
                .patient(patient)
                .dateTime(dateTime)
                .status(AppointmentStatus.PENDING)
                .build();
        Appointment saved = appointmentRepository.save(appt);
        // notifications: doctor and patient
        Notification n1 = Notification.builder()
                .user(doctor.getUser())
                .type(NotificationType.NEW_APPOINTMENT)
                .content("Novo agendamento em " + dateTime)
                .build();
        Notification n2 = Notification.builder()
                .user(patient.getUser())
                .type(NotificationType.NEW_APPOINTMENT)
                .content("Sua consulta foi agendada para " + dateTime)
                .build();
        notificationRepository.save(n1);
        notificationRepository.save(n2);
        return saved;
    }

    public Appointment reschedule(Appointment original, Instant newDateTime) {
        if (original.getStatus() == AppointmentStatus.CANCELED) {
            throw new IllegalArgumentException("Consulta cancelada não pode ser reagendada");
        }
        // Limite de 2 a cada 30 dias
        Instant start = LocalDate.now().minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = Instant.now();
        long count = appointmentRepository.countReschedulesInPeriod(original.getPatient(), start, end);
        if (count >= MAX_RESCHEDULES_IN_30_DAYS) {
            throw new IllegalArgumentException("Reagendamentos excedidos no período de 30 dias");
        }
        Appointment newAppt = schedule(original.getDoctor(), original.getPatient(), newDateTime);
        newAppt.setRescheduledFrom(original);
        return appointmentRepository.save(newAppt);
    }

    public Appointment cancel(Appointment appt) {
        Instant now = Instant.now();
        if (appt.getDateTime().minus(CANCEL_MIN_NOTICE).isBefore(now)) {
            throw new IllegalArgumentException("Cancelamento permitido apenas com antecedência mínima");
        }
        appt.setStatus(AppointmentStatus.CANCELED);
        Appointment saved = appointmentRepository.save(appt);
        Notification n1 = Notification.builder()
                .user(appt.getDoctor().getUser())
                .type(NotificationType.CANCELLATION)
                .content("Agendamento cancelado para " + appt.getDateTime())
                .build();
        Notification n2 = Notification.builder()
                .user(appt.getPatient().getUser())
                .type(NotificationType.CANCELLATION)
                .content("Sua consulta foi cancelada: " + appt.getObservations())
                .build();
        notificationRepository.save(n1);
        notificationRepository.save(n2);
        return saved;
    }

    public Appointment markDone(Appointment appt) {
        appt.setStatus(AppointmentStatus.DONE);
        return appointmentRepository.save(appt);
    }
}

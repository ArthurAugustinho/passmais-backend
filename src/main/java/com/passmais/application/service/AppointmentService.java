package com.passmais.application.service;

import com.passmais.domain.entity.Appointment;
import com.passmais.domain.entity.ConsultationRecord;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.PatientProfile;
import com.passmais.domain.entity.User;
import com.passmais.domain.enums.AppointmentStatus;
import com.passmais.infrastructure.repository.AppointmentRepository;
import com.passmais.infrastructure.repository.NotificationRepository;
import com.passmais.infrastructure.repository.ConsultationRecordRepository;
import com.passmais.domain.entity.Notification;
import com.passmais.domain.enums.NotificationType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final NotificationRepository notificationRepository;
    private final ConsultationRecordRepository consultationRecordRepository;
    private static final int MAX_RESCHEDULES_IN_30_DAYS = 2;
    private static final Duration CANCEL_MIN_NOTICE = Duration.ofHours(6); // antecedência mínima de cancelamento: 6h

    public AppointmentService(AppointmentRepository appointmentRepository,
                              NotificationRepository notificationRepository,
                              ConsultationRecordRepository consultationRecordRepository) {
        this.appointmentRepository = appointmentRepository;
        this.notificationRepository = notificationRepository;
        this.consultationRecordRepository = consultationRecordRepository;
    }

    public Appointment schedule(DoctorProfile doctor,
                                User patientUser,
                                PatientProfile patientProfile,
                                Instant appointmentDateTime,
                                Instant bookingDateTime,
                                String reason,
                                BigDecimal consultationValue,
                                String location,
                                String patientFullName,
                                String patientCpf,
                                LocalDate patientBirthDate,
                                String patientCellPhone) {
        Instant now = Instant.now();
        if (appointmentDateTime.isBefore(now)) {
            throw new IllegalArgumentException("Consulta deve ser no futuro");
        }
        // checar conflito do médico em status relevantes
        if (appointmentRepository.existsByDoctorAndDateTimeAndStatusIn(doctor, appointmentDateTime,
                List.of(AppointmentStatus.PENDING, AppointmentStatus.IN_PROGRESS, AppointmentStatus.DONE))) {
            throw new IllegalArgumentException("Médico indisponível neste horário");
        }
        BigDecimal resolvedValue = consultationValue != null ? consultationValue : doctor.getConsultationPrice();
        if (resolvedValue == null) {
            throw new IllegalArgumentException("Valor da consulta é obrigatório");
        }
        String resolvedPatientName = StringUtils.hasText(patientFullName)
                ? patientFullName
                : Optional.ofNullable(patientProfile)
                .map(PatientProfile::getUser)
                .map(User::getName)
                .orElse(patientUser.getName());
        String resolvedPatientCpf = StringUtils.hasText(patientCpf)
                ? patientCpf
                : Optional.ofNullable(patientProfile).map(PatientProfile::getCpf).orElse(null);
        LocalDate resolvedPatientBirthDate = patientBirthDate != null
                ? patientBirthDate
                : Optional.ofNullable(patientProfile).map(PatientProfile::getBirthDate).orElse(null);
        String resolvedPatientCellPhone = StringUtils.hasText(patientCellPhone)
                ? patientCellPhone
                : Optional.ofNullable(patientProfile).map(PatientProfile::getCellPhone).orElse(null);
        String resolvedLocation = StringUtils.hasText(location) ? location : doctor.getClinicStreetAndNumber();
        Instant bookingMoment = bookingDateTime != null ? bookingDateTime : now;

        Appointment appt = Appointment.builder()
                .doctor(doctor)
                .patient(patientUser)
                .dateTime(appointmentDateTime)
                .reason(reason)
                .status(AppointmentStatus.PENDING)
                .value(resolvedValue)
                .bookedAt(bookingMoment)
                .patientFullName(resolvedPatientName)
                .patientCpf(resolvedPatientCpf)
                .patientBirthDate(resolvedPatientBirthDate)
                .patientCellPhone(resolvedPatientCellPhone)
                .location(resolvedLocation)
                .build();
        Appointment saved = appointmentRepository.save(appt);
        // notifications: doctor and patient
        ConsultationRecord record = ConsultationRecord.builder()
                .appointment(saved)
                .status(com.passmais.domain.enums.ConsultationRecordStatus.DRAFT)
                .reason(saved.getReason())
                .symptomDuration(saved.getSymptomDuration())
                .lastSavedAt(now)
                .build();
        consultationRecordRepository.save(record);

        Notification n1 = Notification.builder()
                .user(doctor.getUser())
                .type(NotificationType.NEW_APPOINTMENT)
                .content("Novo agendamento em " + appointmentDateTime)
                .build();
        Notification n2 = Notification.builder()
                .user(patientUser)
                .type(NotificationType.NEW_APPOINTMENT)
                .content("Sua consulta foi agendada para " + appointmentDateTime)
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
        Appointment newAppt = schedule(
                original.getDoctor(),
                original.getPatient(),
                null,
                newDateTime,
                Instant.now(),
                original.getReason(),
                original.getValue(),
                original.getLocation(),
                original.getPatientFullName(),
                original.getPatientCpf(),
                original.getPatientBirthDate(),
                original.getPatientCellPhone()
        );
        newAppt.setRescheduledFrom(original);
        return appointmentRepository.save(newAppt);
    }

    public Appointment cancel(Appointment appt, String cancelReason) {
        Instant now = Instant.now();
        if (appt.getStatus() == AppointmentStatus.CANCELED) {
            throw new IllegalArgumentException("Consulta já foi cancelada.");
        }
        if (appt.getDateTime().minus(CANCEL_MIN_NOTICE).isBefore(now)) {
            throw new IllegalArgumentException("Cancelamento permitido apenas com antecedência mínima");
        }
        appt.setStatus(AppointmentStatus.CANCELED);
        appt.setCanceledAt(now);
        String resolvedReason = cancelReason;
        if (!StringUtils.hasText(resolvedReason)) {
            resolvedReason = appt.getObservations();
        }
        appt.setCanceledReason(resolvedReason);
        Appointment saved = appointmentRepository.save(appt);
        Notification n1 = Notification.builder()
                .user(appt.getDoctor().getUser())
                .type(NotificationType.CANCELLATION)
                .content("Agendamento cancelado para " + appt.getDateTime())
                .build();
        Notification n2 = Notification.builder()
                .user(appt.getPatient())
                .type(NotificationType.CANCELLATION)
                .content("Sua consulta foi cancelada." + (resolvedReason != null ? " Motivo: " + resolvedReason : ""))
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

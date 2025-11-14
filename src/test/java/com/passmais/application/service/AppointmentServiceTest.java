package com.passmais.application.service;

import com.passmais.domain.entity.Appointment;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.PatientProfile;
import com.passmais.domain.entity.User;
import com.passmais.domain.enums.AppointmentStatus;
import com.passmais.domain.enums.Role;
import com.passmais.infrastructure.repository.AppointmentRepository;
import com.passmais.infrastructure.repository.ConsultationRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import com.passmais.infrastructure.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class AppointmentServiceTest {

    private AppointmentRepository repo;
    private AppointmentService service;
    private NotificationRepository notificationRepository;
    private ConsultationRecordRepository consultationRecordRepository;

    @BeforeEach
    void setup() {
        repo = Mockito.mock(AppointmentRepository.class);
        notificationRepository = Mockito.mock(NotificationRepository.class);
        consultationRecordRepository = Mockito.mock(ConsultationRecordRepository.class);
        service = new AppointmentService(repo, notificationRepository, consultationRecordRepository);
        when(consultationRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void scheduleShouldRejectPastDate() {
        DoctorProfile d = new DoctorProfile();
        User patientUser = User.builder().id(UUID.randomUUID()).name("Paciente Teste").role(Role.PATIENT).build();
        PatientProfile p = PatientProfile.builder().user(patientUser).cpf("12345678901").birthDate(LocalDate.of(1990, 1, 1)).cellPhone("11999999999").build();
        Instant past = Instant.now().minus(Duration.ofHours(1));
        assertThrows(IllegalArgumentException.class, () -> service.schedule(
                d,
                patientUser,
                p,
                past,
                Instant.now(),
                null,
                BigDecimal.TEN,
                "Clinica Central",
                "Paciente Teste",
                "12345678901",
                LocalDate.of(1990, 1, 1),
                "11999999999"
        ));
    }

    @Test
    void cancelShouldRejectLateCancellation() {
        Appointment a = new Appointment();
        a.setDateTime(Instant.now().plus(Duration.ofHours(1))); // menos que janela de 6h
        assertThrows(IllegalArgumentException.class, () -> service.cancel(a, null));
    }

    @Test
    void scheduleOkWhenNoConflict() {
        DoctorProfile d = new DoctorProfile();
        User patientUser = User.builder().id(UUID.randomUUID()).name("Paciente Teste").role(Role.PATIENT).build();
        PatientProfile p = PatientProfile.builder().user(patientUser).cpf("12345678901").birthDate(LocalDate.of(1990, 1, 1)).cellPhone("11999999999").build();
        Instant future = Instant.now().plus(Duration.ofHours(24));
        when(repo.existsByDoctorAndDateTimeAndStatusIn(any(), any(), anyList())).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Appointment appt = service.schedule(
                d,
                patientUser,
                p,
                future,
                Instant.now(),
                "Consulta de rotina",
                BigDecimal.valueOf(150),
                "Clinica Central",
                "Paciente Teste",
                "12345678901",
                LocalDate.of(1990, 1, 1),
                "11999999999"
        );
        assertEquals(AppointmentStatus.PENDING, appt.getStatus());
        assertEquals(future, appt.getDateTime());
        assertEquals(BigDecimal.valueOf(150), appt.getValue());
        assertEquals("Clinica Central", appt.getLocation());
        assertEquals(patientUser, appt.getPatient());
    }
}

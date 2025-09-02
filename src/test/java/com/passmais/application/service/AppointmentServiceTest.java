package com.passmais.application.service;

import com.passmais.domain.entity.Appointment;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.PatientProfile;
import com.passmais.domain.enums.AppointmentStatus;
import com.passmais.infrastructure.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class AppointmentServiceTest {

    private AppointmentRepository repo;
    private AppointmentService service;

    @BeforeEach
    void setup() {
        repo = Mockito.mock(AppointmentRepository.class);
        service = new AppointmentService(repo);
    }

    @Test
    void scheduleShouldRejectPastDate() {
        DoctorProfile d = new DoctorProfile();
        PatientProfile p = new PatientProfile();
        Instant past = Instant.now().minus(Duration.ofHours(1));
        assertThrows(IllegalArgumentException.class, () -> service.schedule(d, p, past));
    }

    @Test
    void cancelShouldRejectLateCancellation() {
        Appointment a = new Appointment();
        a.setDateTime(Instant.now().plus(Duration.ofHours(1))); // menos que janela de 6h
        assertThrows(IllegalArgumentException.class, () -> service.cancel(a));
    }

    @Test
    void scheduleOkWhenNoConflict() {
        DoctorProfile d = new DoctorProfile();
        PatientProfile p = new PatientProfile();
        Instant future = Instant.now().plus(Duration.ofHours(24));
        when(repo.existsByDoctorAndDateTimeAndStatusIn(any(), any(), anyList())).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Appointment appt = service.schedule(d, p, future);
        assertEquals(AppointmentStatus.PENDING, appt.getStatus());
        assertEquals(future, appt.getDateTime());
    }
}

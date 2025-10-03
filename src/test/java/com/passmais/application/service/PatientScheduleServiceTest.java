package com.passmais.application.service;

import com.passmais.application.service.exception.ScheduleException;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.DoctorAvailableSlot;
import com.passmais.domain.entity.DoctorScheduleSlot;
import com.passmais.domain.enums.AvailableSlotStatus;
import com.passmais.domain.enums.ScheduleSlotSource;
import com.passmais.infrastructure.repository.DoctorProfileRepository;
import com.passmais.infrastructure.repository.DoctorScheduleSlotRepository;
import com.passmais.infrastructure.repository.DoctorAvailableSlotRepository;
import com.passmais.interfaces.dto.schedule.PatientScheduleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PatientScheduleServiceTest {

    private DoctorProfileRepository doctorProfileRepository;
    private DoctorScheduleSlotRepository slotRepository;
    private DoctorAvailableSlotRepository availableSlotRepository;
    private PatientScheduleService service;

    @BeforeEach
    void setUp() {
        doctorProfileRepository = mock(DoctorProfileRepository.class);
        slotRepository = mock(DoctorScheduleSlotRepository.class);
        availableSlotRepository = mock(DoctorAvailableSlotRepository.class);
        service = new PatientScheduleService(doctorProfileRepository, slotRepository, availableSlotRepository, 14, 60);
    }

    @Test
    void shouldReturnGroupedSlots() {
        UUID doctorId = UUID.randomUUID();
        DoctorProfile doctor = new DoctorProfile();
        doctor.setId(doctorId);
        com.passmais.domain.entity.User user = new com.passmais.domain.entity.User();
        user.setName("Dr. Teste");
        doctor.setUser(user);
        doctor.setSpecialty("Cardiologia");
        doctor.setCrm("CRM123");
        when(doctorProfileRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        LocalDate start = LocalDate.now().plusDays(1);
        DoctorScheduleSlot slot1 = DoctorScheduleSlot.builder()
                .doctor(doctor)
                .date(start)
                .time(LocalTime.of(9, 0))
                .source(ScheduleSlotSource.RECURRING)
                .build();
        DoctorScheduleSlot slot2 = DoctorScheduleSlot.builder()
                .doctor(doctor)
                .date(start.plusDays(2))
                .time(LocalTime.of(10, 0))
                .source(ScheduleSlotSource.SPECIFIC)
                .build();
        when(slotRepository.findActiveByDoctorAndDateRange(eq(doctorId), any(), any()))
                .thenReturn(List.of(slot1, slot2));

        DoctorAvailableSlot available1 = DoctorAvailableSlot.builder()
                .doctor(doctor)
                .slotDate(start)
                .startTime(LocalTime.of(9, 0))
                .status(AvailableSlotStatus.AVAILABLE)
                .build();
        DoctorAvailableSlot available2 = DoctorAvailableSlot.builder()
                .doctor(doctor)
                .slotDate(start)
                .startTime(LocalTime.of(9, 30))
                .status(AvailableSlotStatus.AVAILABLE)
                .build();
        DoctorAvailableSlot available3 = DoctorAvailableSlot.builder()
                .doctor(doctor)
                .slotDate(start.plusDays(2))
                .startTime(LocalTime.of(10, 0))
                .status(AvailableSlotStatus.AVAILABLE)
                .build();
        when(availableSlotRepository.findByDoctorAndSlotDateBetweenOrderByStartAtUtc(eq(doctor), any(), any()))
                .thenReturn(List.of(available1, available2, available3));

        PatientScheduleResponse response = service.getDoctorSchedule(doctorId, start, start);

        assertEquals(doctorId.toString(), response.doctorId());
        assertEquals("Dr. Teste", response.doctorName());
        assertEquals("Cardiologia", response.doctorSpecialty());
        assertEquals("CRM123", response.doctorCrm());
        assertEquals(start.toString(), response.startDate());
        assertEquals(start.plusDays(6).toString(), response.endDate());
        assertEquals(7, response.days().size());

        var firstDay = response.days().stream()
                .filter(day -> day.isoDate().equals(start.toString()))
                .findFirst()
                .orElseThrow();
        assertEquals(2, firstDay.slots().size());
        assertFalse(firstDay.blocked());

        var emptyDay = response.days().stream()
                .filter(day -> day.isoDate().equals(start.plusDays(1).toString()))
                .findFirst()
                .orElseThrow();
        assertTrue(emptyDay.slots().isEmpty());
        assertFalse(emptyDay.blocked());

        var thirdDay = response.days().stream()
                .filter(day -> day.isoDate().equals(start.plusDays(2).toString()))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("10:00"), thirdDay.slots());
    }

    @Test
    void shouldRejectEndBeforeStart() {
        UUID doctorId = UUID.randomUUID();
        when(doctorProfileRepository.findById(doctorId)).thenReturn(Optional.of(new DoctorProfile()));

        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end = start.minusDays(1);

        ScheduleException ex = assertThrows(ScheduleException.class, () -> service.getDoctorSchedule(doctorId, start, end));
        assertEquals("INVALID_RANGE", ex.getCode());
        verify(slotRepository, never()).findActiveByDoctorAndDateRange(any(), any(), any());
        verify(availableSlotRepository, never()).findByDoctorAndSlotDateBetweenOrderByStartAtUtc(any(), any(), any());
    }

    @Test
    void shouldFallbackToScheduleSlotsWhenNoAvailableSlots() {
        UUID doctorId = UUID.randomUUID();
        DoctorProfile doctor = new DoctorProfile();
        doctor.setId(doctorId);
        when(doctorProfileRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        LocalDate start = LocalDate.now().plusDays(2);
        DoctorScheduleSlot slotMorning = DoctorScheduleSlot.builder()
                .doctor(doctor)
                .date(start)
                .time(LocalTime.of(9, 0))
                .source(ScheduleSlotSource.SPECIFIC)
                .build();
        DoctorScheduleSlot slotAfternoon = DoctorScheduleSlot.builder()
                .doctor(doctor)
                .date(start)
                .time(LocalTime.of(15, 30))
                .source(ScheduleSlotSource.SPECIFIC)
                .build();
        when(slotRepository.findActiveByDoctorAndDateRange(eq(doctorId), any(), any()))
                .thenReturn(List.of(slotMorning, slotAfternoon));

        when(availableSlotRepository.findByDoctorAndSlotDateBetweenOrderByStartAtUtc(eq(doctor), any(), any()))
                .thenReturn(List.of());

        PatientScheduleResponse response = service.getDoctorSchedule(doctorId, start, start.plusDays(3));

        var day = response.days().stream()
                .filter(d -> d.isoDate().equals(start.toString()))
                .findFirst()
                .orElseThrow();

        assertEquals(List.of("09:00", "15:30"), day.slots());
        assertFalse(day.blocked());
    }
}

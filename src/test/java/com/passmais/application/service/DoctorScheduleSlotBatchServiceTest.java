package com.passmais.application.service;

import com.passmais.application.service.exception.ScheduleException;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.DoctorScheduleSlot;
import com.passmais.domain.enums.ScheduleSlotSource;
import com.passmais.infrastructure.repository.DoctorProfileRepository;
import com.passmais.infrastructure.repository.DoctorScheduleSlotRepository;
import com.passmais.interfaces.dto.schedule.ScheduleBatchDayRequest;
import com.passmais.interfaces.dto.schedule.ScheduleBatchUpsertResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DoctorScheduleSlotBatchServiceTest {

    private DoctorProfileRepository doctorProfileRepository;
    private DoctorScheduleSlotRepository slotRepository;
    private DoctorScheduleSlotBatchService service;

    @BeforeEach
    void setUp() {
        doctorProfileRepository = mock(DoctorProfileRepository.class);
        slotRepository = mock(DoctorScheduleSlotRepository.class);
        service = new DoctorScheduleSlotBatchService(doctorProfileRepository, slotRepository, 96, false);
    }

    @Test
    void upsertShouldCreateSlotsAndReturnCreated() {
        UUID doctorId = UUID.randomUUID();
        DoctorProfile doctor = new DoctorProfile();
        doctor.setId(doctorId);

        when(doctorProfileRepository.findByIdForUpdate(doctorId)).thenReturn(Optional.of(doctor));
        when(slotRepository.softDeleteActiveByDate(eq(doctorId), any(), any(), eq(doctorId), anyString())).thenReturn(0);
        when(slotRepository.findMaxVersion(eq(doctorId), any())).thenReturn(0);

        List<ScheduleBatchDayRequest> payload = List.of(
                new ScheduleBatchDayRequest(
                        LocalDate.now().plusDays(1).toString(),
                        "label",
                        ScheduleSlotSource.RECURRING.name().toLowerCase(),
                        List.of("08:00", "08:30")
                )
        );

        ScheduleBatchUpsertResult result = service.upsert(doctorId, doctorId, payload);

        assertEquals(201, result.status().value());
        ScheduleBatchUpsertResponse body = result.body();
        assertEquals(doctorId.toString(), body.doctorId());
        assertEquals(1, body.receivedDays());
        assertEquals(2, body.createdSlots());
        assertEquals(0, body.blockedDays());
        assertFalse(body.replacedPreviousVersions());
        assertEquals(1, body.days().size());
        assertEquals(2, body.days().get(0).slotsCreated());

        verify(slotRepository).saveAll(argThat(iterable -> {
            List<DoctorScheduleSlot> slots = new ArrayList<>();
            iterable.forEach(slots::add);
            return slots.size() == 2 && slots.stream().allMatch(slot -> slot.getDoctor().equals(doctor));
        }));
    }

    @Test
    void upsertShouldRejectDuplicateDate() {
        UUID doctorId = UUID.randomUUID();
        DoctorProfile doctor = new DoctorProfile();
        doctor.setId(doctorId);

        when(doctorProfileRepository.findByIdForUpdate(doctorId)).thenReturn(Optional.of(doctor));

        String date = LocalDate.now().plusDays(1).toString();
        List<ScheduleBatchDayRequest> payload = List.of(
                new ScheduleBatchDayRequest(date, null, "recurring", List.of("08:00")),
                new ScheduleBatchDayRequest(date, null, "recurring", List.of("09:00"))
        );

        ScheduleException ex = assertThrows(ScheduleException.class, () -> service.upsert(doctorId, doctorId, payload));
        assertEquals("DUPLICATE_DATE", ex.getCode());
        verify(slotRepository, never()).saveAll(any());
    }
}

package com.passmais.application.service;

import com.passmais.application.service.exception.ScheduleException;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.DoctorAvailableSlot;
import com.passmais.domain.entity.DoctorScheduleSlot;
import com.passmais.domain.enums.AvailableSlotStatus;
import com.passmais.domain.enums.ScheduleSlotSource;
import com.passmais.infrastructure.repository.DoctorAvailableSlotRepository;
import com.passmais.infrastructure.repository.DoctorProfileRepository;
import com.passmais.infrastructure.repository.DoctorScheduleSlotRepository;
import com.passmais.interfaces.dto.schedule.PatientScheduleDayResponse;
import com.passmais.interfaces.dto.schedule.PatientScheduleResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class PatientScheduleService {

    private static final ZoneId TIMEZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter LABEL_FORMATTER = DateTimeFormatter.ofPattern("EEEE, dd/MM", new Locale("pt", "BR"));

    private final DoctorProfileRepository doctorProfileRepository;
    private final DoctorScheduleSlotRepository slotRepository;
    private final DoctorAvailableSlotRepository availableSlotRepository;
    private final int weekLengthDays;
    private final int maxRangeDays;

    public PatientScheduleService(DoctorProfileRepository doctorProfileRepository,
                                  DoctorScheduleSlotRepository slotRepository,
                                  DoctorAvailableSlotRepository availableSlotRepository,
                                  @Value("${schedule.patient.week-length-days:7}") int weekLengthDays,
                                  @Value("${schedule.patient.max-range-days:60}") int maxRangeDays) {
        this.doctorProfileRepository = doctorProfileRepository;
        this.slotRepository = slotRepository;
        this.availableSlotRepository = availableSlotRepository;
        this.weekLengthDays = Math.max(weekLengthDays, 1);
        this.maxRangeDays = Math.max(maxRangeDays, this.weekLengthDays);
    }

    public PatientScheduleResponse getDoctorSchedule(UUID doctorId,
                                                     LocalDate startDate,
                                                     LocalDate endDate) {
        DoctorProfile doctor = doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new ScheduleException(HttpStatus.NOT_FOUND, "DOCTOR_NOT_FOUND", "Médico não encontrado"));

        LocalDate today = LocalDate.now(TIMEZONE);
        LocalDate effectiveStart = startDate != null ? startDate : today;
        if (effectiveStart.isBefore(today)) {
            effectiveStart = today;
        }
        LocalDate requestedEnd = endDate != null ? endDate : effectiveStart.plusDays(weekLengthDays - 1L);
        if (requestedEnd.isBefore(effectiveStart)) {
            throw new ScheduleException(HttpStatus.BAD_REQUEST, "INVALID_RANGE", "endDate não pode ser anterior a startDate");
        }

        LocalDate effectiveEnd = effectiveStart.plusDays(weekLengthDays - 1L);
        if (endDate != null && requestedEnd.isAfter(effectiveEnd)) {
            throw new ScheduleException(HttpStatus.BAD_REQUEST, "RANGE_TOO_LARGE", "Intervalo solicitado deve cobrir no máximo uma semana", Map.of(
                    "weekLengthDays", weekLengthDays
            ));
        }
        long range = java.time.temporal.ChronoUnit.DAYS.between(effectiveStart, effectiveEnd) + 1L;
        if (range > maxRangeDays) {
            throw new ScheduleException(HttpStatus.BAD_REQUEST, "RANGE_TOO_LARGE", "Intervalo de consulta excede o limite permitido", Map.of(
                    "maxRangeDays", maxRangeDays
            ));
        }

        List<DoctorScheduleSlot> scheduleSlots = slotRepository.findActiveByDoctorAndDateRange(doctorId, effectiveStart, effectiveEnd);
        Map<LocalDate, List<DoctorScheduleSlot>> scheduleByDate = new LinkedHashMap<>();
        for (DoctorScheduleSlot slot : scheduleSlots) {
            scheduleByDate.computeIfAbsent(slot.getDate(), d -> new ArrayList<>()).add(slot);
        }

        List<DoctorAvailableSlot> availableSlots = availableSlotRepository
                .findByDoctorAndSlotDateBetweenOrderByStartAtUtc(doctor, effectiveStart, effectiveEnd);
        Map<LocalDate, List<DoctorAvailableSlot>> availableByDate = new LinkedHashMap<>();
        for (DoctorAvailableSlot slot : availableSlots) {
            availableByDate.computeIfAbsent(slot.getSlotDate(), d -> new ArrayList<>()).add(slot);
        }

        List<PatientScheduleDayResponse> days = new ArrayList<>();
        for (LocalDate current = effectiveStart; !current.isAfter(effectiveEnd); current = current.plusDays(1)) {
            List<DoctorScheduleSlot> daySchedule = scheduleByDate.getOrDefault(current, List.of());
            List<DoctorAvailableSlot> dayAvailable = availableByDate.getOrDefault(current, List.of());

            boolean blocked = !daySchedule.isEmpty() && daySchedule.stream().allMatch(s -> s.getTime() == null);
            List<String> times = dayAvailable.stream()
                    .filter(slot -> slot.getStatus() == null || slot.getStatus() == AvailableSlotStatus.AVAILABLE)
                    .map(DoctorAvailableSlot::getStartTime)
                    .filter(time -> time != null)
                    .sorted()
                    .map(LocalTime::toString)
                    .toList();

            ScheduleSlotSource source = daySchedule.stream()
                    .map(DoctorScheduleSlot::getSource)
                    .findFirst()
                    .orElse(blocked ? ScheduleSlotSource.NONE : ScheduleSlotSource.SPECIFIC);
            days.add(new PatientScheduleDayResponse(
                    current.toString(),
                    LABEL_FORMATTER.format(current),
                    source.name().toLowerCase(Locale.ROOT),
                    times,
                    blocked
            ));
        }

        return new PatientScheduleResponse(
                doctorId.toString(),
                doctor.getUser() != null ? doctor.getUser().getName() : null,
                doctor.getSpecialty(),
                doctor.getCrm(),
                TIMEZONE.getId(),
                effectiveStart.toString(),
                effectiveEnd.toString(),
                days
        );
    }
}

package com.passmais.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.passmais.domain.entity.*;
import com.passmais.domain.enums.AppointmentStatus;
import com.passmais.domain.enums.AvailableSlotStatus;
import com.passmais.domain.enums.ScheduleMode;
import com.passmais.domain.enums.ScheduleSlotSource;
import com.passmais.domain.schedule.ScheduleSlot;
import com.passmais.infrastructure.repository.*;
import com.passmais.interfaces.dto.schedule.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoctorScheduleService {

    private static final int DEFAULT_SLOT_GENERATION_RANGE_DAYS = 30;
    private static final ZoneId UTC = ZoneOffset.UTC;
    private static final Map<String, DayOfWeek> WEEKDAY_BY_KEY;
    private static final Map<DayOfWeek, String> KEY_BY_WEEKDAY;
    private static final List<AppointmentStatus> BLOCKING_STATUSES = List.of(
            AppointmentStatus.PENDING,
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.IN_PROGRESS,
            AppointmentStatus.DONE
    );

    static {
        Map<String, DayOfWeek> map = new HashMap<>();
        map.put("SEGUNDA-FEIRA", DayOfWeek.MONDAY);
        map.put("TERCA-FEIRA", DayOfWeek.TUESDAY);
        map.put("TERÇA-FEIRA", DayOfWeek.TUESDAY);
        map.put("QUARTA-FEIRA", DayOfWeek.WEDNESDAY);
        map.put("QUINTA-FEIRA", DayOfWeek.THURSDAY);
        map.put("SEXTA-FEIRA", DayOfWeek.FRIDAY);
        map.put("SABADO", DayOfWeek.SATURDAY);
        map.put("SÁBADO", DayOfWeek.SATURDAY);
        map.put("DOMINGO", DayOfWeek.SUNDAY);
        for (DayOfWeek dow : DayOfWeek.values()) {
            map.putIfAbsent(dow.name(), dow);
        }
        WEEKDAY_BY_KEY = Collections.unmodifiableMap(map);

        Map<DayOfWeek, String> reverse = new EnumMap<>(DayOfWeek.class);
        reverse.put(DayOfWeek.MONDAY, "Segunda-feira");
        reverse.put(DayOfWeek.TUESDAY, "Terça-feira");
        reverse.put(DayOfWeek.WEDNESDAY, "Quarta-feira");
        reverse.put(DayOfWeek.THURSDAY, "Quinta-feira");
        reverse.put(DayOfWeek.FRIDAY, "Sexta-feira");
        reverse.put(DayOfWeek.SATURDAY, "Sábado");
        reverse.put(DayOfWeek.SUNDAY, "Domingo");
        KEY_BY_WEEKDAY = Collections.unmodifiableMap(reverse);
    }

    private final DoctorProfileRepository doctorProfileRepository;
    private final DoctorScheduleStateRepository stateRepository;
    private final DoctorScheduleSpecificSettingsRepository specificSettingsRepository;
    private final DoctorScheduleSpecificRepository specificRepository;
    private final DoctorScheduleRecurringSettingsRepository recurringSettingsRepository;
    private final DoctorScheduleRecurringRepository recurringRepository;
    private final DoctorScheduleExceptionRepository exceptionRepository;
    private final DoctorAvailableSlotRepository availableSlotRepository;
    private final ScheduleAuditLogRepository auditLogRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public DoctorScheduleService(DoctorProfileRepository doctorProfileRepository,
                                 DoctorScheduleStateRepository stateRepository,
                                 DoctorScheduleSpecificSettingsRepository specificSettingsRepository,
                                 DoctorScheduleSpecificRepository specificRepository,
                                 DoctorScheduleRecurringSettingsRepository recurringSettingsRepository,
                                 DoctorScheduleRecurringRepository recurringRepository,
                                 DoctorScheduleExceptionRepository exceptionRepository,
                                 DoctorAvailableSlotRepository availableSlotRepository,
                                 ScheduleAuditLogRepository auditLogRepository,
                                 AppointmentRepository appointmentRepository,
                                 UserRepository userRepository,
                                 ObjectMapper objectMapper) {
        this.doctorProfileRepository = doctorProfileRepository;
        this.stateRepository = stateRepository;
        this.specificSettingsRepository = specificSettingsRepository;
        this.specificRepository = specificRepository;
        this.recurringSettingsRepository = recurringSettingsRepository;
        this.recurringRepository = recurringRepository;
        this.exceptionRepository = exceptionRepository;
        this.availableSlotRepository = availableSlotRepository;
        this.auditLogRepository = auditLogRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public DoctorSchedulePayload getSchedule(UUID doctorId) {
        DoctorProfile doctor = doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new EntityNotFoundException("Doctor not found"));

        ScheduleMode mode = stateRepository.findByDoctor(doctor)
                .map(state -> state.getMode())
                .orElse(ScheduleMode.RECURRING);

        SpecificSchedulePayload specific = buildSpecificPayload(doctor);
        RecurringSchedulePayload recurring = buildRecurringPayload(doctor);
        RecurringGlobalSettingsPayload recurringGlobal = buildRecurringGlobalPayload(doctor);

        return new DoctorSchedulePayload(mode, specific, recurring, recurringGlobal);
    }

    @Transactional
    public DoctorSchedulePayload saveSchedule(UUID doctorId, DoctorSchedulePayload payload, UUID actorUserId) {
        DoctorProfile doctor = doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new EntityNotFoundException("Doctor not found"));

        DoctorSchedulePayload before = getSchedule(doctorId);
        validatePayload(payload);

        upsertState(doctor, payload.mode());
        upsertSpecific(doctor, payload.specific());
        upsertRecurring(doctor, payload.recurring(), payload.recurringSettings());

        regenerateFutureSlots(doctor);
        DoctorSchedulePayload after = getSchedule(doctorId);
        registerAuditLog(doctor, before, after, actorUserId, "update");

        return after;
    }

    @Transactional
    public DoctorSchedulePayload manageException(UUID doctorId,
                                                 ScheduleExceptionRequest request,
                                                 UUID actorUserId) {
        if (request == null || request.exceptionDate() == null) {
            throw new IllegalArgumentException("exceptionDate é obrigatório");
        }

        DoctorProfile doctor = doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new EntityNotFoundException("Doctor not found"));

        DoctorSchedulePayload before = getSchedule(doctorId);

        boolean remove = Boolean.TRUE.equals(request.remove());
        LocalDate exceptionDate = request.exceptionDate();

        if (remove) {
            exceptionRepository.deleteByDoctorAndExceptionDate(doctor, exceptionDate);
        } else {
            DoctorScheduleException exception = exceptionRepository.findByDoctor(doctor).stream()
                    .filter(e -> exceptionDate.equals(e.getExceptionDate()))
                    .findFirst()
                    .orElseGet(() -> {
                        DoctorScheduleException created = new DoctorScheduleException();
                        created.setDoctor(doctor);
                        created.setExceptionDate(exceptionDate);
                        return created;
                    });
            exception.setDescription(request.description());
            exceptionRepository.save(exception);
        }

        DoctorSchedulePayload after = getSchedule(doctorId);
        regenerateFutureSlots(doctor);
        registerAuditLog(doctor, before, after, actorUserId, remove ? "exception_remove" : "exception_add");
        return after;
    }

    private void upsertState(DoctorProfile doctor, ScheduleMode mode) {
        if (mode == null) {
            mode = ScheduleMode.RECURRING;
        }
        DoctorScheduleState state = stateRepository.findByDoctor(doctor)
                .orElseGet(() -> {
                    DoctorScheduleState created = new DoctorScheduleState();
                    created.setDoctor(doctor);
                    return created;
                });
        state.setMode(mode);
        stateRepository.save(state);
    }

    private void upsertSpecific(DoctorProfile doctor, SpecificSchedulePayload specific) {
        specificRepository.deleteByDoctor(doctor);
        if (specific == null || specific.settings() == null || specific.days() == null || specific.days().isEmpty()) {
            specificSettingsRepository.findById(doctor.getId()).ifPresent(specificSettingsRepository::delete);
            return;
        }

        SpecificScheduleSettingsPayload settings = specific.settings();
        int appointmentInterval = Optional.ofNullable(settings.appointmentInterval()).orElse(30);
        int bufferMinutes = Optional.ofNullable(settings.bufferMinutes()).orElse(0);

        DoctorScheduleSpecificSettings entity = specificSettingsRepository.findById(doctor.getId())
                .orElseGet(() -> {
                    DoctorScheduleSpecificSettings created = new DoctorScheduleSpecificSettings();
                    created.setDoctor(doctor);
                    return created;
                });
        entity.setAppointmentInterval(appointmentInterval);
        entity.setBufferMinutes(bufferMinutes);
        specificSettingsRepository.save(entity);

        specific.days().forEach((key, dayPayload) -> {
            LocalDate date = LocalDate.parse(key);
            List<ScheduleSlot> slots = mapSlots(dayPayload.slots(), appointmentInterval, bufferMinutes);
            DoctorScheduleSpecific specificDay = new DoctorScheduleSpecific();
            specificDay.setDoctor(doctor);
            specificDay.setDate(date);
            specificDay.setSlots(slots);
            specificRepository.save(specificDay);
        });
    }

    private void upsertRecurring(DoctorProfile doctor, RecurringSchedulePayload recurring, RecurringGlobalSettingsPayload global) {
        recurringRepository.deleteByDoctor(doctor);
        exceptionRepository.deleteByDoctor(doctor);

        if (recurring == null || recurring.settings() == null) {
            recurringSettingsRepository.findById(doctor.getId()).ifPresent(recurringSettingsRepository::delete);
        } else {
            RecurringScheduleSettingsPayload settings = recurring.settings();
            int appointmentInterval = Optional.ofNullable(settings.appointmentInterval()).orElse(30);
            int bufferMinutes = Optional.ofNullable(settings.bufferMinutes()).orElse(0);

            DoctorScheduleRecurringSettings entity = recurringSettingsRepository.findById(doctor.getId())
                    .orElseGet(() -> {
                        DoctorScheduleRecurringSettings created = new DoctorScheduleRecurringSettings();
                        created.setDoctor(doctor);
                        return created;
                    });
            entity.setAppointmentInterval(appointmentInterval);
            entity.setBufferMinutes(bufferMinutes);
            entity.setStartDate(settings.startDate());
            entity.setEndDate(settings.endDate());
            entity.setNoEndDate(Boolean.TRUE.equals(settings.noEndDate()));

            boolean enabled = global != null && Boolean.TRUE.equals(global.enabled());
            boolean active = global == null || Boolean.TRUE.equals(global.isRecurringActive());
            entity.setEnabled(enabled);
            entity.setRecurringActive(active);
            recurringSettingsRepository.save(entity);

            if (settings.exceptions() != null) {
                for (LocalDate exceptionDate : settings.exceptions()) {
                    DoctorScheduleException exception = new DoctorScheduleException();
                    exception.setDoctor(doctor);
                    exception.setExceptionDate(exceptionDate);
                    exceptionRepository.save(exception);
                }
            }
        }

        if (recurring == null || recurring.schedule() == null || recurring.schedule().isEmpty()) {
            return;
        }

        RecurringScheduleSettingsPayload settings = recurring.settings();
        int appointmentInterval = Optional.ofNullable(settings.appointmentInterval()).orElse(30);
        int bufferMinutes = Optional.ofNullable(settings.bufferMinutes()).orElse(0);

        recurring.schedule().forEach((key, dayPayload) -> {
            DayOfWeek dow = resolveDayOfWeek(key);
            boolean enabled = dayPayload.enabled() != null && dayPayload.enabled();
            List<ScheduleSlot> slots = mapSlots(dayPayload.slots(), appointmentInterval, bufferMinutes);
            DoctorScheduleRecurring rule = new DoctorScheduleRecurring();
            rule.setDoctor(doctor);
            rule.setWeekday(dow);
            rule.setEnabled(enabled);
            rule.setSlots(slots);
            recurringRepository.save(rule);
        });
    }

    private SpecificSchedulePayload buildSpecificPayload(DoctorProfile doctor) {
        Optional<DoctorScheduleSpecificSettings> settingsOpt = specificSettingsRepository.findById(doctor.getId());
        List<DoctorScheduleSpecific> days = specificRepository.findByDoctorOrderByDateAsc(doctor);
        if (settingsOpt.isEmpty() && days.isEmpty()) {
            return null;
        }
        SpecificScheduleSettingsPayload settingsPayload = settingsOpt
                .map(s -> new SpecificScheduleSettingsPayload(s.getAppointmentInterval(), s.getBufferMinutes()))
                .orElse(null);

        Map<String, SpecificDayPayload> daysMap = days.stream()
                .collect(Collectors.toMap(
                        day -> day.getDate().toString(),
                        day -> new SpecificDayPayload(day.getSlots().stream()
                                .map(this::toPayload)
                                .toList()),
                        (a, b) -> b,
                        LinkedHashMap::new
                ));

        return new SpecificSchedulePayload(settingsPayload, daysMap);
    }

    private RecurringSchedulePayload buildRecurringPayload(DoctorProfile doctor) {
        Optional<DoctorScheduleRecurringSettings> settingsOpt = recurringSettingsRepository.findById(doctor.getId());
        List<DoctorScheduleRecurring> rules = recurringRepository.findByDoctor(doctor);
        if (settingsOpt.isEmpty() && rules.isEmpty()) {
            return null;
        }

        DoctorScheduleRecurringSettings settings = settingsOpt.orElse(null);
        RecurringScheduleSettingsPayload settingsPayload = settings == null ? null :
                new RecurringScheduleSettingsPayload(
                        settings.getAppointmentInterval(),
                        settings.getBufferMinutes(),
                        settings.getStartDate(),
                        settings.getEndDate(),
                        settings.isNoEndDate(),
                        exceptionRepository.findByDoctor(doctor).stream()
                                .map(e -> e.getExceptionDate())
                                .sorted()
                                .toList()
                );

        Map<String, RecurringDayPayload> schedule = new LinkedHashMap<>();
        for (DoctorScheduleRecurring rule : rules) {
            String key = KEY_BY_WEEKDAY.getOrDefault(rule.getWeekday(), rule.getWeekday().name());
            RecurringDayPayload payload = new RecurringDayPayload(
                    rule.isEnabled(),
                    rule.getSlots().stream().map(this::toPayload).toList()
            );
            schedule.put(key, payload);
        }
        return new RecurringSchedulePayload(settingsPayload, schedule);
    }

    private RecurringGlobalSettingsPayload buildRecurringGlobalPayload(DoctorProfile doctor) {
        return recurringSettingsRepository.findById(doctor.getId())
                .map(settings -> new RecurringGlobalSettingsPayload(
                        settings.isEnabled(),
                        settings.isRecurringActive()
                ))
                .orElse(null);
    }

    private ScheduleSlotPayload toPayload(ScheduleSlot slot) {
        if (slot == null) {
            return null;
        }
        return new ScheduleSlotPayload(
                slot.id(),
                slot.start(),
                slot.end(),
                slot.intervalMinutes(),
                slot.endBufferMinutes()
        );
    }

    private List<ScheduleSlot> mapSlots(List<ScheduleSlotPayload> slots,
                                        int defaultInterval,
                                        int defaultBuffer) {
        if (slots == null) {
            return List.of();
        }
        return slots.stream()
                .map(slot -> new ScheduleSlot(
                        slot.id(),
                        slot.start(),
                        slot.end(),
                        Optional.ofNullable(slot.interval()).orElse(defaultInterval),
                        slot.endBuffer()
                ))
                .toList();
    }

    private DayOfWeek resolveDayOfWeek(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Dia da semana não pode ser nulo");
        }
        String normalized = key.trim().toUpperCase(Locale.ROOT);
        DayOfWeek dow = WEEKDAY_BY_KEY.get(normalized);
        if (dow == null) {
            throw new IllegalArgumentException("Dia da semana inválido: " + key);
        }
        return dow;
    }

    private void validatePayload(DoctorSchedulePayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload de horários não pode ser nulo");
        }
        validateSpecific(payload.specific());
        validateRecurring(payload.recurring());
    }

    private void validateSpecific(SpecificSchedulePayload specific) {
        if (specific == null) {
            return;
        }
        SpecificScheduleSettingsPayload settings = specific.settings();
        if (settings == null) {
            throw new IllegalArgumentException("Configurações específicas precisam definir os intervalos");
        }
        int interval = Optional.ofNullable(settings.appointmentInterval()).orElseThrow(() ->
                new IllegalArgumentException("appointmentInterval obrigatório para horários específicos"));
        int buffer = Optional.ofNullable(settings.bufferMinutes()).orElse(0);
        if (interval <= 0) {
            throw new IllegalArgumentException("appointmentInterval deve ser positivo");
        }
        if (buffer < 0) {
            throw new IllegalArgumentException("bufferMinutes não pode ser negativo");
        }
        if (specific.days() == null) {
            return;
        }
        specific.days().forEach((dateStr, day) -> {
            LocalDate.parse(dateStr);
            ensureSlotsChronology(day.slots(), interval, buffer);
        });
    }

    private void validateRecurring(RecurringSchedulePayload recurring) {
        if (recurring == null) {
            return;
        }
        RecurringScheduleSettingsPayload settings = recurring.settings();
        if (settings == null) {
            throw new IllegalArgumentException("Configurações recorrentes são obrigatórias");
        }
        int interval = Optional.ofNullable(settings.appointmentInterval()).orElseThrow(() ->
                new IllegalArgumentException("appointmentInterval obrigatório para horários recorrentes"));
        int buffer = Optional.ofNullable(settings.bufferMinutes()).orElse(0);
        if (interval <= 0) {
            throw new IllegalArgumentException("appointmentInterval deve ser positivo");
        }
        if (buffer < 0) {
            throw new IllegalArgumentException("bufferMinutes não pode ser negativo");
        }
        if (Boolean.TRUE.equals(settings.noEndDate()) && settings.endDate() != null) {
            throw new IllegalArgumentException("Não é permitido informar endDate quando noEndDate=true");
        }
        if (!Boolean.TRUE.equals(settings.noEndDate()) && settings.startDate() != null && settings.endDate() != null
                && settings.endDate().isBefore(settings.startDate())) {
            throw new IllegalArgumentException("endDate não pode ser anterior ao startDate");
        }
        if (recurring.schedule() == null) {
            return;
        }
        recurring.schedule().forEach((dayKey, day) -> {
            resolveDayOfWeek(dayKey);
            ensureSlotsChronology(day.slots(), interval, buffer);
        });
    }

    private void ensureSlotsChronology(List<ScheduleSlotPayload> slots, int fallbackInterval, int fallbackBuffer) {
        if (slots == null || slots.isEmpty()) {
            return;
        }
        List<ScheduleSlotPayload> ordered = slots.stream()
                .sorted(Comparator.comparing(ScheduleSlotPayload::start))
                .toList();
        LocalTime lastEnd = null;
        for (ScheduleSlotPayload slot : ordered) {
            if (slot.start() == null || slot.end() == null) {
                throw new IllegalArgumentException("Horários precisam ter início e fim");
            }
            if (!slot.start().isBefore(slot.end())) {
                throw new IllegalArgumentException("Horário inicial deve ser anterior ao final");
            }
            if (lastEnd != null && !slot.start().isAfter(lastEnd)) {
                throw new IllegalArgumentException("Faixas de horários não podem se sobrepor");
            }
            int interval = Optional.ofNullable(slot.interval()).orElse(fallbackInterval);
            if (interval <= 0) {
                throw new IllegalArgumentException("Intervalo precisa ser positivo");
            }
            if (Optional.ofNullable(slot.endBuffer()).orElse(0) < 0) {
                throw new IllegalArgumentException("endBuffer não pode ser negativo");
            }
            lastEnd = slot.end();
        }
    }

    private void regenerateFutureSlots(DoctorProfile doctor) {
        LocalDate startDate = LocalDate.now(UTC);
        LocalDate endDate = startDate.plusDays(DEFAULT_SLOT_GENERATION_RANGE_DAYS - 1L);

        availableSlotRepository.deleteFutureSlotsFrom(doctor, startDate);

        Map<LocalDate, DoctorScheduleSpecific> specificByDate = specificRepository.findByDoctorOrderByDateAsc(doctor)
                .stream().collect(Collectors.toMap(day -> day.getDate(), it -> it));

        DoctorScheduleRecurringSettings recurringSettings = recurringSettingsRepository.findById(doctor.getId()).orElse(null);
        Map<DayOfWeek, DoctorScheduleRecurring> recurringByDay = recurringRepository.findByDoctor(doctor).stream()
                .collect(Collectors.toMap(rule -> rule.getWeekday(), it -> it));
        Set<LocalDate> exceptions = exceptionRepository.findByDoctor(doctor).stream()
                .map(e -> e.getExceptionDate())
                .collect(Collectors.toSet());

        List<DoctorAvailableSlot> slotsToPersist = new ArrayList<>();

        for (LocalDate current = startDate; !current.isAfter(endDate); current = current.plusDays(1)) {
            DoctorScheduleSpecific specific = specificByDate.get(current);
            List<ScheduleSlot> windows = null;
            int interval = 0;
            int buffer = 0;
            ScheduleSlotSource source;

            if (specific != null && !specific.getSlots().isEmpty()) {
                DoctorScheduleSpecificSettings settings = specificSettingsRepository.findById(doctor.getId()).orElse(null);
                interval = settings != null ? settings.getAppointmentInterval() : 30;
                buffer = settings != null ? settings.getBufferMinutes() : 0;
                windows = specific.getSlots();
                source = ScheduleSlotSource.SPECIFIC;
            } else if (shouldApplyRecurring(current, recurringSettings, exceptions)) {
                DoctorScheduleRecurring rule = recurringByDay.get(current.getDayOfWeek());
                if (rule == null || !rule.isEnabled() || rule.getSlots().isEmpty()) {
                    continue;
                }
                interval = recurringSettings != null ? recurringSettings.getAppointmentInterval() : 30;
                buffer = recurringSettings != null ? recurringSettings.getBufferMinutes() : 0;
                windows = rule.getSlots();
                source = ScheduleSlotSource.RECURRING;
            } else {
                continue;
            }

            int effectiveInterval = Math.max(interval, 1);
            int effectiveBuffer = Math.max(buffer, 0);

            for (ScheduleSlot window : windows) {
                generateSlotsForWindow(doctor, current, window, effectiveInterval, effectiveBuffer, source, slotsToPersist);
            }
        }

        availableSlotRepository.saveAll(slotsToPersist);
    }

    private boolean shouldApplyRecurring(LocalDate date,
                                         DoctorScheduleRecurringSettings settings,
                                         Set<LocalDate> exceptions) {
        if (settings == null) {
            return false;
        }
        if (!settings.isEnabled() || !settings.isRecurringActive()) {
            return false;
        }
        if (exceptions != null && exceptions.contains(date)) {
            return false;
        }
        if (settings.getStartDate() != null && date.isBefore(settings.getStartDate())) {
            return false;
        }
        if (!settings.isNoEndDate() && settings.getEndDate() != null && date.isAfter(settings.getEndDate())) {
            return false;
        }
        return true;
    }

    private void generateSlotsForWindow(DoctorProfile doctor,
                                        LocalDate date,
                                        ScheduleSlot window,
                                        int defaultInterval,
                                        int defaultBuffer,
                                        ScheduleSlotSource source,
                                        List<DoctorAvailableSlot> accumulator) {
        if (window == null) {
            return;
        }
        LocalTime start = window.start();
        LocalTime end = window.end();
        if (start == null || end == null || !start.isBefore(end)) {
            return;
        }
        int interval = Optional.ofNullable(window.intervalMinutes()).orElse(defaultInterval);
        int buffer = Math.max(defaultBuffer, 0);
        int endBuffer = Math.max(Optional.ofNullable(window.endBufferMinutes()).orElse(0), 0);
        LocalTime effectiveEnd = end.minusMinutes(endBuffer);
        if (!start.isBefore(effectiveEnd)) {
            return;
        }

        LocalTime cursor = start;
        while (!cursor.isAfter(effectiveEnd)) {
            LocalTime slotEnd = cursor.plusMinutes(interval);
            if (slotEnd.isAfter(effectiveEnd)) {
                break;
            }
            Instant startUtc = ZonedDateTime.of(date, cursor, UTC).toInstant();
            Instant endUtc = ZonedDateTime.of(date, slotEnd, UTC).toInstant();

            boolean blocked = appointmentRepository.existsByDoctorAndDateTimeAndStatusIn(doctor, startUtc, BLOCKING_STATUSES);

            DoctorAvailableSlot slot = new DoctorAvailableSlot();
            slot.setDoctor(doctor);
            slot.setSlotDate(date);
            slot.setStartTime(cursor);
            slot.setEndTime(slotEnd);
            slot.setStartAtUtc(startUtc);
            slot.setEndAtUtc(endUtc);
            slot.setSource(source);
            slot.setStatus(blocked ? AvailableSlotStatus.BLOCKED : AvailableSlotStatus.AVAILABLE);
            accumulator.add(slot);

            cursor = cursor.plusMinutes(interval + buffer);
        }
    }

    private void registerAuditLog(DoctorProfile doctor,
                                  DoctorSchedulePayload before,
                                  DoctorSchedulePayload after,
                                  UUID actorUserId,
                                  String changeType) {
        ScheduleAuditLog log = new ScheduleAuditLog();
        log.setDoctor(doctor);
        log.setChangeType(changeType);
        log.setOldSchedule(toJson(before));
        log.setNewSchedule(toJson(after));
        log.setChangedBy(actorUserId == null ? null : userRepository.findById(actorUserId).orElse(null));
        auditLogRepository.save(log);
    }

    private String toJson(DoctorSchedulePayload payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível serializar a auditoria de horários", e);
        }
    }
}

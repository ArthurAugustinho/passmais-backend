package com.passmais.application.service;

import com.passmais.application.service.exception.ScheduleException;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.DoctorScheduleSlot;
import com.passmais.domain.enums.ScheduleSlotSource;
import com.passmais.infrastructure.repository.DoctorProfileRepository;
import com.passmais.infrastructure.repository.DoctorScheduleSlotRepository;
import com.passmais.interfaces.dto.schedule.ScheduleBatchDayRequest;
import com.passmais.interfaces.dto.schedule.ScheduleBatchDayResponse;
import com.passmais.interfaces.dto.schedule.ScheduleBatchUpsertResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DoctorScheduleSlotBatchService {

    private static final ZoneId TIMEZONE = ZoneId.of("America/Sao_Paulo");
    private static final String TIMEZONE_ID = "America/Sao_Paulo";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String DELETE_REASON = "replaced_by_upload";

    private final DoctorProfileRepository doctorProfileRepository;
    private final DoctorScheduleSlotRepository slotRepository;
    private final int maxSlotsPerDay;
    private final boolean requireFutureSlots;

    public DoctorScheduleSlotBatchService(DoctorProfileRepository doctorProfileRepository,
                                          DoctorScheduleSlotRepository slotRepository,
                                          @Value("${schedule.batch.max-slots-per-day:96}") int maxSlotsPerDay,
                                          @Value("${schedule.batch.require-future-slots:false}") boolean requireFutureSlots) {
        this.doctorProfileRepository = doctorProfileRepository;
        this.slotRepository = slotRepository;
        this.maxSlotsPerDay = maxSlotsPerDay;
        this.requireFutureSlots = requireFutureSlots;
    }

    @Transactional
    public ScheduleBatchUpsertResult upsert(UUID doctorId,
                                            UUID actorDoctorId,
                                            List<ScheduleBatchDayRequest> requestDays) {
        if (requestDays == null || requestDays.isEmpty()) {
            throw new ScheduleException(HttpStatus.BAD_REQUEST, "INVALID_PAYLOAD", "Nenhum dia informado");
        }

        DoctorProfile doctor = doctorProfileRepository.findByIdForUpdate(doctorId)
                .orElseThrow(() -> new ScheduleException(HttpStatus.NOT_FOUND, "DOCTOR_NOT_FOUND", "Médico não encontrado"));

        List<ValidatedDay> days = validate(requestDays);

        Instant now = Instant.now();
        int totalCreatedSlots = 0;
        int blockedDays = 0;
        boolean replacedPreviousVersions = false;
        List<ScheduleBatchDayResponse> responses = new ArrayList<>();

        for (ValidatedDay day : days) {
            int deleted = slotRepository.softDeleteActiveByDate(doctorId, day.date(), now, actorDoctorId, DELETE_REASON);
            boolean hadPrevious = deleted > 0;
            if (hadPrevious) {
                replacedPreviousVersions = true;
            }

            int nextVersion = slotRepository.findMaxVersion(doctorId, day.date()) + 1;

            if (day.blocked()) {
                DoctorScheduleSlot slot = DoctorScheduleSlot.builder()
                        .doctor(doctor)
                        .date(day.date())
                        .time(null)
                        .source(ScheduleSlotSource.NONE)
                        .version(nextVersion)
                        .createdBy(actorDoctorId)
                        .deleteReason(null)
                        .deletedAt(null)
                        .deletedBy(null)
                        .build();
                slotRepository.save(slot);
                blockedDays++;
                responses.add(new ScheduleBatchDayResponse(
                        day.isoDate(),
                        day.source().name().toLowerCase(Locale.ROOT),
                        0,
                        hadPrevious,
                        Boolean.TRUE
                ));
            } else {
                List<DoctorScheduleSlot> entities = day.slots().stream()
                        .map(time -> DoctorScheduleSlot.builder()
                                .doctor(doctor)
                                .date(day.date())
                                .time(time)
                                .source(day.source())
                                .version(nextVersion)
                                .createdBy(actorDoctorId)
                                .deleteReason(null)
                                .deletedAt(null)
                                .deletedBy(null)
                                .build())
                        .collect(Collectors.toCollection(ArrayList::new));
                slotRepository.saveAll(entities);
                totalCreatedSlots += entities.size();
                responses.add(new ScheduleBatchDayResponse(
                        day.isoDate(),
                        day.source().name().toLowerCase(Locale.ROOT),
                        entities.size(),
                        hadPrevious,
                        null
                ));
            }
        }

        ScheduleBatchUpsertResponse body = new ScheduleBatchUpsertResponse(
                doctorId.toString(),
                TIMEZONE_ID,
                requestDays.size(),
                totalCreatedSlots,
                blockedDays,
                replacedPreviousVersions,
                responses
        );

        HttpStatus status = replacedPreviousVersions ? HttpStatus.OK : HttpStatus.CREATED;
        return new ScheduleBatchUpsertResult(body, status);
    }

    private List<ValidatedDay> validate(List<ScheduleBatchDayRequest> requestDays) {
        Map<LocalDate, ValidatedDay> byDate = new LinkedHashMap<>();
        ZonedDateTime now = ZonedDateTime.now(TIMEZONE);

        for (int i = 0; i < requestDays.size(); i++) {
            ScheduleBatchDayRequest raw = requestDays.get(i);
            if (raw == null) {
                throw new ScheduleException(HttpStatus.BAD_REQUEST, "INVALID_ITEM", "Item da posição " + i + " é nulo");
            }

            String isoDate = trim(raw.isoDate());
            if (isoDate == null || isoDate.isEmpty()) {
                throw new ScheduleException(HttpStatus.BAD_REQUEST, "INVALID_DATE", "isoDate é obrigatório");
            }

            LocalDate date;
            try {
                date = LocalDate.parse(isoDate);
            } catch (Exception ex) {
                throw new ScheduleException(HttpStatus.BAD_REQUEST, "INVALID_DATE", "isoDate inválido", Map.of("value", isoDate));
            }

            String sourceRaw = trim(raw.source());
            if (sourceRaw == null || sourceRaw.isEmpty()) {
                throw new ScheduleException(HttpStatus.BAD_REQUEST, "INVALID_SOURCE", "source é obrigatório", Map.of("date", isoDate));
            }

            ScheduleSlotSource source;
            try {
                source = ScheduleSlotSource.valueOf(sourceRaw.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new ScheduleException(HttpStatus.BAD_REQUEST, "INVALID_SOURCE", "source inválido", Map.of("value", sourceRaw, "date", isoDate));
            }

            List<String> rawSlots = raw.slots();
            if (rawSlots == null) {
                throw new ScheduleException(HttpStatus.BAD_REQUEST, "INVALID_SLOTS", "slots é obrigatório", Map.of("date", isoDate));
            }

            boolean blocked = source == ScheduleSlotSource.NONE;
            if (blocked && !rawSlots.isEmpty()) {
                throw new ScheduleException(HttpStatus.BAD_REQUEST, "INVALID_SLOTS", "slots deve ser vazio quando source=none", Map.of("date", isoDate));
            }
            if (!blocked && rawSlots.isEmpty()) {
                throw new ScheduleException(HttpStatus.BAD_REQUEST, "INVALID_SLOTS", "slots não pode ser vazio", Map.of("date", isoDate));
            }

            List<LocalTime> slots = new ArrayList<>();
            Set<LocalTime> unique = new HashSet<>();
            for (String slotValue : rawSlots) {
                String value = trim(slotValue);
                if (value == null || value.isEmpty()) {
                    throw new ScheduleException(HttpStatus.BAD_REQUEST, "INVALID_SLOT", "Horário vazio", Map.of("date", isoDate));
                }
                LocalTime time;
                try {
                    time = LocalTime.parse(value, TIME_FORMATTER);
                } catch (Exception ex) {
                    throw new ScheduleException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_SLOT", "Horário inválido", Map.of("value", value, "date", isoDate));
                }

                if (!unique.add(time)) {
                    throw new ScheduleException(HttpStatus.UNPROCESSABLE_ENTITY, "DUPLICATE_SLOT", "Horário duplicado", Map.of("slot", value, "date", isoDate));
                }

                if (requireFutureSlots) {
                    ZonedDateTime slotDateTime = ZonedDateTime.of(date, time, TIMEZONE);
                    if (!slotDateTime.isAfter(now)) {
                        throw new ScheduleException(HttpStatus.UNPROCESSABLE_ENTITY, "PAST_SLOT", "Horário no passado", Map.of("slot", value, "date", isoDate));
                    }
                }

                slots.add(time);
            }

            if (!blocked && maxSlotsPerDay > 0 && slots.size() > maxSlotsPerDay) {
                throw new ScheduleException(HttpStatus.UNPROCESSABLE_ENTITY, "SLOTS_LIMIT_EXCEEDED",
                        "Quantidade de horários excede o limite", Map.of("date", isoDate, "limit", maxSlotsPerDay));
            }

            slots.sort(Comparator.naturalOrder());

            if (byDate.containsKey(date)) {
                throw new ScheduleException(HttpStatus.BAD_REQUEST, "DUPLICATE_DATE", "isoDate duplicado no payload", Map.of("date", isoDate));
            }

            byDate.put(date, new ValidatedDay(isoDate, date, source, slots, blocked));
        }

        return new ArrayList<>(byDate.values());
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private record ValidatedDay(String isoDate,
                                LocalDate date,
                                ScheduleSlotSource source,
                                List<LocalTime> slots,
                                boolean blocked) {
    }
}

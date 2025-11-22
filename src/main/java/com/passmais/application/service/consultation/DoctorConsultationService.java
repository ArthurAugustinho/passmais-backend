package com.passmais.application.service.consultation;

import com.passmais.domain.entity.Appointment;
import com.passmais.domain.entity.ConsultationRecord;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.PatientAlert;
import com.passmais.domain.entity.PatientFile;
import com.passmais.domain.entity.User;
import com.passmais.domain.enums.AppointmentStatus;
import com.passmais.domain.enums.ConsultationRecordStatus;
import com.passmais.domain.exception.ApiErrorException;
import com.passmais.infrastructure.repository.AppointmentRepository;
import com.passmais.infrastructure.repository.ConsultationRecordRepository;
import com.passmais.infrastructure.repository.DoctorProfileRepository;
import com.passmais.infrastructure.repository.PatientAlertRepository;
import com.passmais.infrastructure.repository.PatientFileRepository;
import com.passmais.infrastructure.repository.UserRepository;
import com.passmais.interfaces.dto.consultation.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DoctorConsultationService {

    private static final int MIN_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final AppointmentRepository appointmentRepository;
    private final ConsultationRecordRepository consultationRecordRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientAlertRepository patientAlertRepository;
    private final PatientFileRepository patientFileRepository;
    private final UserRepository userRepository;

    public DoctorConsultationService(AppointmentRepository appointmentRepository,
                                     ConsultationRecordRepository consultationRecordRepository,
                                     DoctorProfileRepository doctorProfileRepository,
                                     PatientAlertRepository patientAlertRepository,
                                     PatientFileRepository patientFileRepository,
                                     UserRepository userRepository) {
        this.appointmentRepository = appointmentRepository;
        this.consultationRecordRepository = consultationRecordRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.patientAlertRepository = patientAlertRepository;
        this.patientFileRepository = patientFileRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public DoctorConsultationPageResponseDTO listDoctorAppointments(UUID doctorId,
                                                                    Instant from,
                                                                    Instant to,
                                                                    List<AppointmentStatus> statuses,
                                                                    String patientQuery,
                                                                    Integer page,
                                                                    Integer pageSize,
                                                                    String sortParam,
                                                                    UUID actorUserId) {
        DoctorProfile doctor = requireDoctor(doctorId);
        ensureDoctorOwnership(doctor, actorUserId);

        int resolvedPage = (page == null || page < MIN_PAGE) ? MIN_PAGE : page;
        int resolvedSize = (pageSize == null || pageSize <= 0) ? DEFAULT_PAGE_SIZE : pageSize;

        Specification<Appointment> spec = Specification.where(byDoctor(doctorId));
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dateTime"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("dateTime"), to));
        }
        if (statuses != null && !statuses.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("status").in(statuses));
        }
        if (patientQuery != null && !patientQuery.isBlank()) {
            String like = "%" + patientQuery.toLowerCase(Locale.ROOT).trim() + "%";
            spec = spec.and((root, query, cb) -> {
                var patientJoin = root.join("patient");
                query.distinct(true);
                return cb.like(cb.lower(patientJoin.get("name")), like);
            });
        }

        Pageable pageable = PageRequest.of(resolvedPage - 1, resolvedSize, resolveSort(sortParam));
        Page<Appointment> result = appointmentRepository.findAll(spec, pageable);

        Map<UUID, List<PatientAlert>> alertsByPatient = fetchAlertsByPatient(result.getContent());
        Map<String, Instant> presenceByCpf = fetchPresenceByCpf(result.getContent());

        List<DoctorConsultationListItemDTO> items = result.getContent().stream()
                .map(appt -> {
                    Instant presence = presenceByCpf.getOrDefault(sanitizeDigits(appt.getPatientCpf()), null);
                    return toListItem(appt, alertsByPatient.getOrDefault(appt.getPatient().getId(), List.of()), true, presence);
                })
                .toList();

        return new DoctorConsultationPageResponseDTO(items, resolvedPage, resolvedSize, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public DetailResult getConsultation(UUID doctorId,
                                        UUID appointmentId,
                                        UUID actorUserId) {
        DoctorProfile doctor = requireDoctor(doctorId);
        ensureDoctorOwnership(doctor, actorUserId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ApiErrorException("CONSULTATION_NOT_FOUND", "Consulta não encontrada", HttpStatus.NOT_FOUND));
        if (!appointment.getDoctor().getId().equals(doctorId)) {
            throw new ApiErrorException("CONSULTATION_NOT_OWNED", "Consulta não pertence ao médico", HttpStatus.FORBIDDEN);
        }

        ConsultationRecord record = ensureConsultationRecord(appointment);

        List<ClinicalAlertDTO> alerts = toAlertDtos(fetchAlertsByPatient(List.of(appointment))
                .getOrDefault(appointment.getPatient().getId(), List.of()));
        Instant presenceConfirmedAt = findPresenceForAppointment(appointment);

        DoctorConsultationDetailDTO body = new DoctorConsultationDetailDTO(
                appointment.getId(),
                appointment.getDateTime(),
                ConsultationStatusMapper.toApiAppointmentStatus(appointment.getStatus()),
                appointment.getReason(),
                appointment.getSymptomDuration(),
                appointment.getPreConsultNotes(),
                toRecordDto(record, appointment),
                alerts,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                toPatientSummary(appointment, true, presenceConfirmedAt)
        );
        return new DetailResult(body, buildEtag(record));
    }

    @Transactional
    public AutosaveResult autosaveRecord(UUID doctorId,
                                         UUID appointmentId,
                                         UUID actorUserId,
                                         ConsultationRecordPatchRequest request,
                                         String ifMatch) {
        DoctorProfile doctor = requireDoctor(doctorId);
        ensureDoctorOwnership(doctor, actorUserId);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ApiErrorException("CONSULTATION_NOT_FOUND", "Consulta não encontrada", HttpStatus.NOT_FOUND));
        ensureOwnership(appointment, doctorId);
        ensureEditable(appointment);

        ConsultationRecord record = ensureConsultationRecord(appointment);
        verifyEtag(record, ifMatch);

        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        boolean changed = applyPatch(record, appointment, request);
        record.setLastSavedAt(Instant.now());
        record.setUpdatedBy(actor);
        record = consultationRecordRepository.saveAndFlush(record);

        if (changed) {
            appointment.setUpdatedBy(actor);
        }
        appointmentRepository.save(appointment);

        ConsultationRecordPatchResponse body = new ConsultationRecordPatchResponse(
                ConsultationStatusMapper.toApiConsultationRecordStatus(record.getStatus()),
                record.getLastSavedAt()
        );
        return new AutosaveResult(body, buildEtag(record));
    }

    @Transactional
    public FinalizeResult finalizeConsultation(UUID doctorId,
                                               UUID appointmentId,
                                               UUID actorUserId,
                                               ConsultationFinalizeRequest request) {
        DoctorProfile doctor = requireDoctor(doctorId);
        ensureDoctorOwnership(doctor, actorUserId);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ApiErrorException("CONSULTATION_NOT_FOUND", "Consulta não encontrada", HttpStatus.NOT_FOUND));
        ensureOwnership(appointment, doctorId);

        if (appointment.getStatus() == AppointmentStatus.CANCELED) {
            throw new ApiErrorException("CONSULTATION_STATUS_BLOCKED", "Consulta cancelada não pode ser finalizada", HttpStatus.CONFLICT);
        }
        if (appointment.getStatus() == AppointmentStatus.DONE) {
            throw new ApiErrorException("CONSULTATION_FINALIZED", "Consulta já finalizada", HttpStatus.CONFLICT);
        }

        ConsultationRecord record = ensureConsultationRecord(appointment);

        Map<String, String> errors = new HashMap<>();
        String reason = resolveValue(request.reason(), Optional.ofNullable(record.getReason()).orElse(appointment.getReason()));
        if (reason == null || reason.isBlank()) {
            errors.put("reason", "Obrigatório");
        }
        String anamnesis = resolveValue(request.anamnesis(), record.getAnamnesis());
        if (anamnesis == null || anamnesis.isBlank()) {
            errors.put("anamnesis", "Obrigatório");
        }
        String physicalExam = resolveValue(request.physicalExam(), record.getPhysicalExam());
        if (physicalExam == null || physicalExam.isBlank()) {
            errors.put("physicalExam", "Obrigatório");
        }
        String plan = resolveValue(request.plan(), record.getPlan());
        if (plan == null || plan.isBlank()) {
            errors.put("plan", "Obrigatório");
        }

        if (!errors.isEmpty()) {
            throw new ApiErrorException("VALIDATION_ERROR", "Campos obrigatórios ausentes", HttpStatus.UNPROCESSABLE_ENTITY, errors);
        }

        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        record.setReason(reason);
        record.setSymptomDuration(resolveValue(request.symptomDuration(), record.getSymptomDuration()));
        record.setAnamnesis(anamnesis);
        record.setPhysicalExam(physicalExam);
        record.setPlan(plan);
        record.setStatus(ConsultationRecordStatus.FINALIZED);
        record.setLastSavedAt(Instant.now());
        record.setUpdatedBy(actor);
        record = consultationRecordRepository.saveAndFlush(record);

        appointment.setReason(reason);
        appointment.setSymptomDuration(record.getSymptomDuration());
        appointment.setUpdatedBy(actor);
        appointment.setStatus(AppointmentStatus.DONE);
        appointment.setFinalizedAt(record.getLastSavedAt());
        appointmentRepository.save(appointment);

        ConsultationFinalizeResponse body = new ConsultationFinalizeResponse(
                ConsultationStatusMapper.toApiAppointmentStatus(appointment.getStatus()),
                appointment.getFinalizedAt()
        );
        return new FinalizeResult(body, buildEtag(record));
    }

    @Transactional
    public ConsultationReopenResponse reopen(UUID doctorId,
                                             UUID appointmentId,
                                             UUID actorUserId) {
        DoctorProfile doctor = requireDoctor(doctorId);
        ensureDoctorOwnership(doctor, actorUserId);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ApiErrorException("CONSULTATION_NOT_FOUND", "Consulta não encontrada", HttpStatus.NOT_FOUND));
        ensureOwnership(appointment, doctorId);

        if (appointment.getStatus() != AppointmentStatus.DONE) {
            throw new ApiErrorException("CONSULTATION_STATUS_BLOCKED", "Apenas consultas concluídas podem ser reabertas", HttpStatus.CONFLICT);
        }

        ConsultationRecord record = ensureConsultationRecord(appointment);
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        record.setStatus(ConsultationRecordStatus.DRAFT);
        record.setLastSavedAt(Instant.now());
        record.setUpdatedBy(actor);
        consultationRecordRepository.save(record);

        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        appointment.setFinalizedAt(null);
        appointment.setUpdatedBy(actor);
        appointmentRepository.save(appointment);

        return new ConsultationReopenResponse(ConsultationStatusMapper.toApiAppointmentStatus(appointment.getStatus()));
    }

    @Transactional(readOnly = true)
    public List<ClinicalAlertDTO> listPatientAlerts(UUID patientId) {
        List<PatientAlert> alerts = patientAlertRepository.findByPatientUserIdAndActiveIsTrue(patientId);
        return toAlertDtos(alerts);
    }

    private DoctorProfile requireDoctor(UUID doctorId) {
        return doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new ApiErrorException("DOCTOR_NOT_FOUND", "Médico não encontrado", HttpStatus.NOT_FOUND));
    }

    private void ensureDoctorOwnership(DoctorProfile doctor, UUID actorUserId) {
        if (actorUserId == null || doctor.getUser() == null || !doctor.getUser().getId().equals(actorUserId)) {
            throw new ApiErrorException("CONSULTATION_NOT_OWNED", "Consulta não pertence ao médico", HttpStatus.FORBIDDEN);
        }
    }

    private void ensureOwnership(Appointment appointment, UUID doctorId) {
        if (!appointment.getDoctor().getId().equals(doctorId)) {
            throw new ApiErrorException("CONSULTATION_NOT_OWNED", "Consulta não pertence ao médico", HttpStatus.FORBIDDEN);
        }
    }

    private void ensureEditable(Appointment appointment) {
        if (appointment.getStatus() == AppointmentStatus.CANCELED) {
            throw new ApiErrorException("CONSULTATION_STATUS_BLOCKED", "Consulta cancelada não pode ser editada", HttpStatus.CONFLICT);
        }
        if (appointment.getStatus() == AppointmentStatus.DONE) {
            throw new ApiErrorException("CONSULTATION_FINALIZED", "Consulta finalizada não pode ser editada", HttpStatus.CONFLICT);
        }
    }

    private Specification<Appointment> byDoctor(UUID doctorId) {
        return (root, query, cb) -> cb.equal(root.get("doctor").get("id"), doctorId);
    }

    private Sort resolveSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "dateTime");
        }
        String[] parts = sortParam.split(":");
        String field = parts[0].trim();
        Sort.Direction direction = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1])) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return switch (field) {
            case "scheduledAt", "dateTime" -> Sort.by(direction, "dateTime");
            case "status" -> Sort.by(direction, "status");
            case "createdAt" -> Sort.by(direction, "createdAt");
            default -> Sort.by(direction, "dateTime");
        };
    }

    private Map<UUID, List<PatientAlert>> fetchAlertsByPatient(Collection<Appointment> appointments) {
        if (appointments == null || appointments.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<UUID> patientUserIds = appointments.stream()
                .map(Appointment::getPatient)
                .filter(Objects::nonNull)
                .map(User::getId)
                .collect(Collectors.toSet());
        if (patientUserIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PatientAlert> alerts = patientAlertRepository.findByPatientUserIdInAndActiveIsTrue(patientUserIds);
        return alerts.stream().collect(Collectors.groupingBy(alert -> alert.getPatient().getUser().getId()));
    }

    private Map<String, Instant> fetchPresenceByCpf(Collection<Appointment> appointments) {
        if (appointments == null || appointments.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> cpfs = appointments.stream()
                .map(Appointment::getPatientCpf)
                .map(this::sanitizeDigits)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (cpfs.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PatientFile> files = patientFileRepository.findByCpfIn(cpfs);
        return files.stream()
                .collect(Collectors.toMap(PatientFile::getCpf, PatientFile::getPresenceConfirmedAt, (a, b) -> a));
    }

    private DoctorConsultationListItemDTO toListItem(Appointment appointment,
                                                     List<PatientAlert> alerts,
                                                     boolean maskSensitive,
                                                     Instant presenceConfirmedAt) {
        return new DoctorConsultationListItemDTO(
                appointment.getId(),
                appointment.getDateTime(),
                ConsultationStatusMapper.toApiAppointmentStatus(appointment.getStatus()),
                appointment.getReason(),
                appointment.getSymptomDuration(),
                toAlertDtos(alerts),
                toPatientSummary(appointment, maskSensitive, presenceConfirmedAt)
        );
    }

    private List<ClinicalAlertDTO> toAlertDtos(List<PatientAlert> alerts) {
        if (alerts == null || alerts.isEmpty()) {
            return List.of();
        }
        return alerts.stream()
                .map(alert -> new ClinicalAlertDTO(
                        alert.getId(),
                        alert.getType().name().toLowerCase(Locale.ROOT),
                        alert.getLabel(),
                        alert.getSeverity().name().toLowerCase(Locale.ROOT)
                ))
                .toList();
    }

    private PatientSummaryDTO toPatientSummary(Appointment appointment, boolean maskSensitive, Instant presenceConfirmedAt) {
        User patientUser = appointment.getPatient();
        if (patientUser == null) {
            return null;
        }
        String cpf = Optional.ofNullable(appointment.getPatientCpf()).orElse(patientUser.getCpf());
        return new PatientSummaryDTO(
                patientUser.getId(),
                Optional.ofNullable(appointment.getPatientFullName()).orElse(patientUser.getName()),
                appointment.getPatientBirthDate(),
                null,
                null,
                cpf,
                presenceConfirmedAt,
                maskSensitive,
                maskSensitive
        );
    }

    private Instant findPresenceForAppointment(Appointment appointment) {
        String cpf = sanitizeDigits(appointment.getPatientCpf());
        if (cpf == null) {
            return null;
        }
        return patientFileRepository.findByCpf(cpf)
                .map(PatientFile::getPresenceConfirmedAt)
                .orElse(null);
    }

    private String sanitizeDigits(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    private ConsultationRecord ensureConsultationRecord(Appointment appointment) {
        return consultationRecordRepository.findByAppointmentId(appointment.getId())
                .orElseGet(() -> {
                    ConsultationRecord created = ConsultationRecord.builder()
                            .appointment(appointment)
                            .status(ConsultationRecordStatus.DRAFT)
                            .reason(appointment.getReason())
                            .symptomDuration(appointment.getSymptomDuration())
                            .lastSavedAt(appointment.getUpdatedAt())
                            .build();
                    return consultationRecordRepository.save(created);
                });
    }

    private ConsultationRecordDTO toRecordDto(ConsultationRecord record, Appointment appointment) {
        return new ConsultationRecordDTO(
                record.getId(),
                ConsultationStatusMapper.toApiConsultationRecordStatus(record.getStatus()),
                Optional.ofNullable(record.getReason()).orElse(appointment.getReason()),
                Optional.ofNullable(record.getSymptomDuration()).orElse(appointment.getSymptomDuration()),
                record.getAnamnesis(),
                record.getPhysicalExam(),
                record.getPlan(),
                record.getLastSavedAt() != null ? record.getLastSavedAt() : record.getUpdatedAt()
        );
    }

    private void verifyEtag(ConsultationRecord record, String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            return;
        }
        String sanitized = ifMatch.trim();
        if (sanitized.startsWith("W/")) {
            sanitized = sanitized.substring(2);
        }
        sanitized = sanitized.replace("\"", "").trim();
        try {
            long provided = Long.parseLong(sanitized);
            if (!Objects.equals(record.getVersion(), provided)) {
                throw new ApiErrorException("CONSULTATION_ETAG_MISMATCH", "Versão desatualizada do prontuário", HttpStatus.PRECONDITION_FAILED);
            }
        } catch (NumberFormatException ex) {
            throw new ApiErrorException("CONSULTATION_ETAG_INVALID", "ETag inválido", HttpStatus.PRECONDITION_FAILED);
        }
    }

    private String buildEtag(ConsultationRecord record) {
        long version = Optional.ofNullable(record.getVersion()).orElse(0L);
        return "\"" + version + "\"";
    }

    private boolean applyPatch(ConsultationRecord record, Appointment appointment, ConsultationRecordPatchRequest request) {
        boolean changed = false;
        if (request.reason() != null) {
            record.setReason(request.reason());
            appointment.setReason(request.reason());
            changed = true;
        }
        if (request.symptomDuration() != null) {
            record.setSymptomDuration(request.symptomDuration());
            appointment.setSymptomDuration(request.symptomDuration());
            changed = true;
        }
        if (request.anamnesis() != null) {
            record.setAnamnesis(request.anamnesis());
            changed = true;
        }
        if (request.physicalExam() != null) {
            record.setPhysicalExam(request.physicalExam());
            changed = true;
        }
        if (request.plan() != null) {
            record.setPlan(request.plan());
            changed = true;
        }
        if (request.status() != null) {
            ConsultationRecordStatus status = ConsultationStatusMapper.fromApiConsultationRecordStatus(request.status());
            if (status == ConsultationRecordStatus.FINALIZED) {
                throw new ApiErrorException("CONSULTATION_STATUS_BLOCKED", "Status inválido para autosave", HttpStatus.CONFLICT);
            }
            record.setStatus(status);
            changed = true;
        }
        return changed;
    }

    private String resolveValue(String payloadValue, String existingValue) {
        return payloadValue != null ? payloadValue : existingValue;
    }

    public record DetailResult(DoctorConsultationDetailDTO body, String etag) {}

    public record AutosaveResult(ConsultationRecordPatchResponse body, String etag) {}

    public record FinalizeResult(ConsultationFinalizeResponse body, String etag) {}
}

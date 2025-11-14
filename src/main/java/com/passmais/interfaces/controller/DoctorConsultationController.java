package com.passmais.interfaces.controller;

import com.passmais.application.service.consultation.ConsultationStatusMapper;
import com.passmais.application.service.consultation.DoctorConsultationService;
import com.passmais.domain.enums.AppointmentStatus;
import com.passmais.domain.enums.Role;
import com.passmais.domain.util.EmailUtils;
import com.passmais.domain.exception.ApiErrorException;
import com.passmais.infrastructure.repository.UserRepository;
import com.passmais.interfaces.dto.consultation.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctor/{doctorId}/appointments")
public class DoctorConsultationController {

    private final DoctorConsultationService doctorConsultationService;
    private final UserRepository userRepository;

    public DoctorConsultationController(DoctorConsultationService doctorConsultationService,
                                        UserRepository userRepository) {
        this.doctorConsultationService = doctorConsultationService;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @GetMapping
    public ResponseEntity<DoctorConsultationPageResponseDTO> listDoctorAppointments(@PathVariable UUID doctorId,
                                                                                    @RequestParam(value = "from", required = false) String from,
                                                                                    @RequestParam(value = "to", required = false) String to,
                                                                                    @RequestParam(value = "status", required = false) String statusCsv,
                                                                                    @RequestParam(value = "patient", required = false) String patient,
                                                                                    @RequestParam(value = "page", required = false) Integer page,
                                                                                    @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                                                    @RequestParam(value = "sort", required = false) String sort) {
        UUID actorUserId = resolveAuthenticatedDoctorUserId()
                .orElseThrow(() -> new ApiErrorException("UNAUTHORIZED", "Usuário não autenticado", HttpStatus.UNAUTHORIZED));

        Instant fromInstant = parseDateParam(from, false);
        Instant toInstant = parseDateParam(to, true);
        List<AppointmentStatus> statuses = parseStatusCsv(statusCsv);

        DoctorConsultationPageResponseDTO response = doctorConsultationService.listDoctorAppointments(
                doctorId,
                fromInstant,
                toInstant,
                statuses,
                patient,
                page,
                pageSize,
                sort,
                actorUserId
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @GetMapping("/{appointmentId}")
    public ResponseEntity<DoctorConsultationDetailDTO> getConsultation(@PathVariable UUID doctorId,
                                                                       @PathVariable UUID appointmentId) {
        UUID actorUserId = resolveAuthenticatedDoctorUserId()
                .orElseThrow(() -> new ApiErrorException("UNAUTHORIZED", "Usuário não autenticado", HttpStatus.UNAUTHORIZED));

        DoctorConsultationService.DetailResult result = doctorConsultationService.getConsultation(doctorId, appointmentId, actorUserId);
        return ResponseEntity.ok().eTag(result.etag()).body(result.body());
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PatchMapping("/{appointmentId}/record")
    public ResponseEntity<ConsultationRecordPatchResponse> autosave(@PathVariable UUID doctorId,
                                                                    @PathVariable UUID appointmentId,
                                                                    @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                                                    @Valid @RequestBody ConsultationRecordPatchRequest request) {
        UUID actorUserId = resolveAuthenticatedDoctorUserId()
                .orElseThrow(() -> new ApiErrorException("UNAUTHORIZED", "Usuário não autenticado", HttpStatus.UNAUTHORIZED));

        DoctorConsultationService.AutosaveResult result = doctorConsultationService.autosaveRecord(
                doctorId,
                appointmentId,
                actorUserId,
                request,
                ifMatch
        );
        return ResponseEntity.ok().eTag(result.etag()).body(result.body());
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping("/{appointmentId}/finalize")
    public ResponseEntity<ConsultationFinalizeResponse> finalizeConsultation(@PathVariable UUID doctorId,
                                                                             @PathVariable UUID appointmentId,
                                                                             @Valid @RequestBody ConsultationFinalizeRequest request) {
        UUID actorUserId = resolveAuthenticatedDoctorUserId()
                .orElseThrow(() -> new ApiErrorException("UNAUTHORIZED", "Usuário não autenticado", HttpStatus.UNAUTHORIZED));

        DoctorConsultationService.FinalizeResult result = doctorConsultationService.finalizeConsultation(
                doctorId,
                appointmentId,
                actorUserId,
                request
        );
        return ResponseEntity.ok().eTag(result.etag()).body(result.body());
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping("/{appointmentId}/reopen")
    public ResponseEntity<ConsultationReopenResponse> reopenConsultation(@PathVariable UUID doctorId,
                                                                         @PathVariable UUID appointmentId) {
        UUID actorUserId = resolveAuthenticatedDoctorUserId()
                .orElseThrow(() -> new ApiErrorException("UNAUTHORIZED", "Usuário não autenticado", HttpStatus.UNAUTHORIZED));

        ConsultationReopenResponse response = doctorConsultationService.reopen(doctorId, appointmentId, actorUserId);
        return ResponseEntity.ok(response);
    }

    private Optional<UUID> resolveAuthenticatedDoctorUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        String username = null;
        if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else if (principal instanceof String s) {
            username = s;
        }
        if (username == null) {
            return Optional.empty();
        }
        String normalized = EmailUtils.normalize(username);
        return userRepository.findByEmailIgnoreCase(normalized != null ? normalized : username)
                .filter(user -> user.getRole() == Role.DOCTOR)
                .map(user -> user.getId());
    }

    private Instant parseDateParam(String value, boolean endOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            try {
                LocalDate date = LocalDate.parse(value);
                if (endOfDay) {
                    return date.plusDays(1)
                            .atStartOfDay(ZoneOffset.UTC)
                            .minusNanos(1)
                            .toInstant();
                }
                return date.atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (DateTimeParseException e2) {
                throw new ApiErrorException("VALIDATION_ERROR", "Data inválida: " + value, HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
    }

    private List<AppointmentStatus> parseStatusCsv(String statusCsv) {
        if (statusCsv == null || statusCsv.isBlank()) {
            return List.of();
        }
        String[] tokens = statusCsv.split(",");
        List<AppointmentStatus> statuses = new ArrayList<>();
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            statuses.add(ConsultationStatusMapper.fromApiAppointmentStatus(trimmed.toLowerCase(Locale.ROOT)));
        }
        return statuses;
    }
}

package com.passmais.interfaces.controller;

import com.passmais.application.service.DoctorScheduleService;
import com.passmais.application.service.DoctorScheduleSlotBatchService;
import com.passmais.application.service.ScheduleBatchUpsertResult;
import com.passmais.application.service.exception.ScheduleException;
import com.passmais.domain.enums.Role;
import com.passmais.infrastructure.repository.DoctorProfileRepository;
import com.passmais.infrastructure.repository.UserRepository;
import com.passmais.interfaces.dto.schedule.DoctorSchedulePayload;
import com.passmais.interfaces.dto.schedule.ScheduleBatchDayRequest;
import com.passmais.interfaces.dto.schedule.ScheduleErrorResponse;
import com.passmais.interfaces.dto.schedule.ScheduleExceptionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctors/{doctorId}/schedule")
public class DoctorScheduleController {

    private final DoctorScheduleService doctorScheduleService;
    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final DoctorScheduleSlotBatchService doctorScheduleSlotBatchService;

    public DoctorScheduleController(DoctorScheduleService doctorScheduleService,
                                    UserRepository userRepository,
                                    DoctorProfileRepository doctorProfileRepository,
                                    DoctorScheduleSlotBatchService doctorScheduleSlotBatchService) {
        this.doctorScheduleService = doctorScheduleService;
        this.userRepository = userRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.doctorScheduleSlotBatchService = doctorScheduleSlotBatchService;
    }

    @PreAuthorize("hasAnyRole('DOCTOR','ADMINISTRATOR')")
    @GetMapping
    public ResponseEntity<DoctorSchedulePayload> getSchedule(@PathVariable UUID doctorId) {
        return ResponseEntity.ok(doctorScheduleService.getSchedule(doctorId));
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping
    public ResponseEntity<?> createSchedule(@PathVariable UUID doctorId,
                                            @RequestBody List<ScheduleBatchDayRequest> request) {
        try {
            DoctorContext context = resolveAuthenticatedDoctorContext();
            if (context == null) {
                throw new ScheduleException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Usuário não autenticado");
            }
            if (!context.doctorId().equals(doctorId)) {
                throw new ScheduleException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Médico não autorizado", Map.of("doctorId", doctorId.toString()));
            }

            ScheduleBatchUpsertResult result = doctorScheduleSlotBatchService.upsert(doctorId, context.doctorId(), request);
            return ResponseEntity.status(result.status()).body(result.body());
        } catch (ScheduleException ex) {
            return buildErrorResponse(ex);
        }
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PutMapping
    public ResponseEntity<?> updateSchedule(@PathVariable UUID doctorId,
                                            @RequestBody DoctorSchedulePayload payload) {
        UUID actorUserId = resolveAuthenticatedUserId();
        if (actorUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuário não autenticado"));
        }
        DoctorSchedulePayload saved = doctorScheduleService.saveSchedule(doctorId, payload, actorUserId);
        return ResponseEntity.ok(saved);
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping("/exception")
    public ResponseEntity<?> manageException(@PathVariable UUID doctorId,
                                             @RequestBody ScheduleExceptionRequest request) {
        UUID actorUserId = resolveAuthenticatedUserId();
        if (actorUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuário não autenticado"));
        }
        DoctorSchedulePayload saved = doctorScheduleService.manageException(doctorId, request, actorUserId);
        return ResponseEntity.ok(saved);
    }

    private UUID resolveAuthenticatedUserId() {
        String username = resolveAuthenticatedUsername();
        if (username == null) {
            return null;
        }
        return userRepository.findByEmail(username)
                .map(user -> user.getId())
                .orElse(null);
    }

    private DoctorContext resolveAuthenticatedDoctorContext() {
        String username = resolveAuthenticatedUsername();
        if (username == null) {
            return null;
        }
        return userRepository.findByEmail(username)
                .filter(user -> user.getRole() == Role.DOCTOR)
                .flatMap(user -> doctorProfileRepository.findByUserId(user.getId())
                        .map(profile -> new DoctorContext(user.getId(), profile.getId())))
                .orElse(null);
    }

    private String resolveAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String s) {
            return s;
        }
        return null;
    }

    private record DoctorContext(UUID userId, UUID doctorId) {
    }

    private ResponseEntity<ScheduleErrorResponse> buildErrorResponse(ScheduleException ex) {
        ScheduleErrorResponse body = new ScheduleErrorResponse(
                "error",
                ex.getCode(),
                ex.getMessage(),
                ex.getDetails()
        );
        return ResponseEntity.status(ex.getStatus()).body(body);
    }
}

package com.passmais.interfaces.controller;

import com.passmais.application.service.PatientAppointmentService;
import com.passmais.domain.entity.Appointment;
import com.passmais.domain.entity.User;
import com.passmais.domain.enums.Role;
import com.passmais.domain.util.EmailUtils;
import com.passmais.infrastructure.repository.UserRepository;
import com.passmais.interfaces.dto.AppointmentResponseDTO;
import com.passmais.interfaces.dto.PatientAppointmentCancelRequestDTO;
import com.passmais.interfaces.dto.PatientAppointmentListItemDTO;
import com.passmais.interfaces.dto.PatientAppointmentRescheduleRequestDTO;
import com.passmais.interfaces.mapper.AppointmentMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients/appointments")
public class PatientAppointmentController {

    private final PatientAppointmentService patientAppointmentService;
    private final UserRepository userRepository;
    private final AppointmentMapper appointmentMapper;

    public PatientAppointmentController(PatientAppointmentService patientAppointmentService,
                                        UserRepository userRepository,
                                        AppointmentMapper appointmentMapper) {
        this.patientAppointmentService = patientAppointmentService;
        this.userRepository = userRepository;
        this.appointmentMapper = appointmentMapper;
    }

    @PreAuthorize("hasRole('PATIENT')")
    @GetMapping
    public ResponseEntity<List<PatientAppointmentListItemDTO>> listOwnAppointments() {
        User patient = requireAuthenticatedPatient();
        return ResponseEntity.ok(patientAppointmentService.listAppointments(patient));
    }

    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping("/{appointmentId}/cancel")
    public ResponseEntity<AppointmentResponseDTO> cancelAppointment(@PathVariable UUID appointmentId,
                                                                    @RequestBody(required = false) PatientAppointmentCancelRequestDTO request) {
        User patient = requireAuthenticatedPatient();
        String reason = request != null ? request.reason() : null;
        try {
            Appointment canceled = patientAppointmentService.cancelAppointment(patient, appointmentId, reason);
            return ResponseEntity.ok(appointmentMapper.toResponse(canceled));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping("/{appointmentId}/reschedule")
    public ResponseEntity<AppointmentResponseDTO> rescheduleAppointment(@PathVariable UUID appointmentId,
                                                                        @RequestBody @Valid PatientAppointmentRescheduleRequestDTO request) {
        User patient = requireAuthenticatedPatient();
        Instant newDateTime = parseDateTime(request);
        Appointment updated = patientAppointmentService.rescheduleAppointment(patient, appointmentId, newDateTime);
        return ResponseEntity.ok(appointmentMapper.toResponse(updated));
    }

    private Instant parseDateTime(PatientAppointmentRescheduleRequestDTO request) {
        LocalDate date;
        LocalTime time;
        try {
            date = LocalDate.parse(request.newDate());
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de data inválido. Use yyyy-MM-dd.");
        }
        try {
            time = LocalTime.parse(request.newTime());
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de horário inválido. Use HH:mm.");
        }
        LocalDateTime localDateTime = LocalDateTime.of(date, time);
        return localDateTime.toInstant(ZoneOffset.UTC);
    }

    private User requireAuthenticatedPatient() {
        return resolveAuthenticatedUser()
                .filter(user -> user.getRole() == Role.PATIENT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Paciente não autenticado."));
    }

    private Optional<User> resolveAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }
        String username = extractUsername(authentication);
        if (username == null) {
            return Optional.empty();
        }
        String normalized = EmailUtils.normalize(username);
        return userRepository.findByEmailIgnoreCase(normalized != null ? normalized : username);
    }

    private String extractUsername(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String s) {
            return s;
        }
        return null;
    }
}

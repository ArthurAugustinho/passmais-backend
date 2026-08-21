package com.passmais.interfaces.controller;

import com.passmais.application.service.SecretaryAppointmentService;
import com.passmais.domain.entity.User;
import com.passmais.domain.enums.AppointmentStatus;
import com.passmais.domain.enums.Role;
import com.passmais.domain.util.EmailUtils;
import com.passmais.infrastructure.repository.UserRepository;
import com.passmais.interfaces.dto.SecretaryAppointmentPageResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctors/{doctorId}/appointments")
public class SecretaryAppointmentController {

    private final SecretaryAppointmentService secretaryAppointmentService;
    private final UserRepository userRepository;

    public SecretaryAppointmentController(SecretaryAppointmentService secretaryAppointmentService,
                                          UserRepository userRepository) {
        this.secretaryAppointmentService = secretaryAppointmentService;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasRole('SECRETARY')")
    @GetMapping
    public ResponseEntity<SecretaryAppointmentPageResponseDTO> listAppointments(@PathVariable UUID doctorId,
                                                                                @RequestParam(value = "from", required = false) String from,
                                                                                @RequestParam(value = "to", required = false) String to,
                                                                                @RequestParam(value = "status", required = false) String statusCsv,
                                                                                @RequestParam(value = "page", required = false) Integer page,
                                                                                @RequestParam(value = "size", required = false) Integer size,
                                                                                @RequestParam(value = "sort", required = false) String sort) {
        User secretary = requireAuthenticatedSecretary();

        Instant fromInstant = parseDateParam(from, false);
        Instant toInstant = parseDateParam(to, true);
        if (fromInstant != null && toInstant != null && fromInstant.isAfter(toInstant)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parâmetro 'from' deve ser anterior ou igual a 'to'");
        }

        List<AppointmentStatus> statuses = parseStatuses(statusCsv);
        Sort resolvedSort = resolveSort(sort);

        SecretaryAppointmentPageResponseDTO response = secretaryAppointmentService.listAppointmentsForDoctor(
                doctorId,
                fromInstant,
                toInstant,
                statuses,
                page,
                size,
                resolvedSort,
                secretary.getId()
        );
        return ResponseEntity.ok(response);
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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data/hora inválida: " + value);
            }
        }
    }

    private List<AppointmentStatus> parseStatuses(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String[] tokens = raw.split("[,|]");
        List<AppointmentStatus> statuses = new ArrayList<>();
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                statuses.add(AppointmentStatus.valueOf(trimmed.toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status inválido: " + trimmed);
            }
        }
        return statuses;
    }

    private Sort resolveSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "dateTime");
        }
        String[] parts = sortParam.split(":", 2);
        String field = parts[0].trim();
        Sort.Direction direction = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1])) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return switch (field) {
            case "dateTime", "scheduledAt" -> Sort.by(direction, "dateTime");
            case "createdAt" -> Sort.by(direction, "createdAt");
            case "bookedAt" -> Sort.by(direction, "bookedAt");
            case "status" -> Sort.by(direction, "status");
            case "finalizedAt" -> Sort.by(direction, "finalizedAt");
            case "value" -> Sort.by(direction, "value");
            default -> Sort.by(direction, "dateTime");
        };
    }

    private User requireAuthenticatedSecretary() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }
        String username = extractUsername(authentication);
        if (username == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }
        String normalized = EmailUtils.normalize(username);
        User user = userRepository.findByEmailIgnoreCase(normalized != null ? normalized : username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado"));
        if (user.getRole() != Role.SECRETARY) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }
        return user;
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

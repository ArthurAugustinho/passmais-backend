package com.passmais.interfaces.controller;

import com.passmais.application.service.consultation.DoctorConsultationService;
import com.passmais.domain.enums.Role;
import com.passmais.domain.exception.ApiErrorException;
import com.passmais.infrastructure.repository.UserRepository;
import com.passmais.interfaces.dto.consultation.ClinicalAlertDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients/{patientId}/alerts")
public class PatientAlertController {

    private final DoctorConsultationService doctorConsultationService;
    private final UserRepository userRepository;

    public PatientAlertController(DoctorConsultationService doctorConsultationService,
                                  UserRepository userRepository) {
        this.doctorConsultationService = doctorConsultationService;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @GetMapping
    public ResponseEntity<List<ClinicalAlertDTO>> listPatientAlerts(@PathVariable UUID patientId) {
        UUID actorUserId = resolveAuthenticatedDoctorUserId()
                .orElseThrow(() -> new ApiErrorException("UNAUTHORIZED", "Usuário não autenticado", HttpStatus.UNAUTHORIZED));
        // No additional ownership rule defined yet; ensure user is a doctor.
        List<ClinicalAlertDTO> alerts = doctorConsultationService.listPatientAlerts(patientId);
        return ResponseEntity.ok(alerts);
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
        return userRepository.findByEmail(username)
                .filter(user -> user.getRole() == Role.DOCTOR)
                .map(user -> user.getId());
    }
}


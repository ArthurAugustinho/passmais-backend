package com.passmais.interfaces.controller;

import com.passmais.application.service.AdminApprovalService;
import com.passmais.domain.entity.Clinic;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.AuditLog;
import com.passmais.domain.entity.User;
import com.passmais.domain.util.EmailUtils;
import com.passmais.infrastructure.repository.AuditLogRepository;
import com.passmais.infrastructure.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminApprovalService adminApprovalService;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AdminController(AdminApprovalService adminApprovalService, AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.adminApprovalService = adminApprovalService;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @PostMapping("/approve/doctor/{id}")
    public ResponseEntity<com.passmais.interfaces.dto.ApproveDoctorResponseDTO> approveDoctor(@PathVariable("id") UUID id) {
        DoctorProfile saved = adminApprovalService.approveDoctor(id);
        var body = new com.passmais.interfaces.dto.ApproveDoctorResponseDTO(
                "Médico aprovado com sucesso",
                new com.passmais.interfaces.dto.ApproveDoctorResponseDTO.Doctor(saved.getId(), saved.getApprovedAt())
        );
        return ResponseEntity.ok(body);
    }

    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @PostMapping("/approve/clinic/{id}")
    public ResponseEntity<com.passmais.interfaces.dto.ApproveClinicResponseDTO> approveClinic(@PathVariable("id") UUID id) {
        Clinic saved = adminApprovalService.approveClinic(id);
        var body = new com.passmais.interfaces.dto.ApproveClinicResponseDTO(
                "Clínica aprovada com sucesso",
                new com.passmais.interfaces.dto.ApproveClinicResponseDTO.Clinic(saved.getId(), saved.getApprovedAt())
        );
        return ResponseEntity.ok(body);
    }

    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @PostMapping("/reject/doctor/{id}")
    public ResponseEntity<?> rejectDoctor(@PathVariable("id") UUID id,
                                          @RequestBody com.passmais.interfaces.dto.RejectDoctorRequestDTO request) {
        String description = request != null ? request.description() : null;
        try {
            UUID actorUserId = resolveAuthenticatedUserId();
            if (actorUserId == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(java.util.Map.of("error", "Unauthorized"));
            }
            DoctorProfile saved = adminApprovalService.rejectDoctor(id, description);
            String logDetails = "Doctor " + id + " rejected" +
                    (saved.getFailureDescription() != null ? ": " + saved.getFailureDescription() : "");
            auditLogRepository.save(
                    AuditLog.builder()
                            .actorUserId(actorUserId)
                            .action("DOCTOR_REJECT")
                            .details(logDetails)
                            .build()
            );
            var body = new com.passmais.interfaces.dto.RejectDoctorResponseDTO(
                    "Doctor rejected successfully",
                    new com.passmais.interfaces.dto.RejectDoctorResponseDTO.Doctor(
                            saved.getId(),
                            saved.getRejectedAt(),
                            saved.getFailureDescription()
                    )
            );
            return ResponseEntity.ok(body);
        } catch (java.util.NoSuchElementException ex) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(java.util.Map.of("error", "Doctor not found."));
        } catch (Exception ex) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to reject doctor. Please try again later."));
        }
    }

    private UUID resolveAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        String username = null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else if (principal instanceof String s) {
            username = s;
        }
        if (username == null) {
            return null;
        }
        String normalized = EmailUtils.normalize(username);
        return userRepository.findByEmailIgnoreCase(normalized != null ? normalized : username)
                .map(User::getId)
                .orElse(null);
    }

    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @PostMapping("/reject/clinic/{id}")
    public ResponseEntity<com.passmais.interfaces.dto.RejectClinicResponseDTO> rejectClinic(@PathVariable("id") UUID id, @RequestParam(name = "adminUserId") UUID adminUserId, @RequestParam(name = "reason", required = false) String reason) {
        Clinic saved = adminApprovalService.rejectClinic(id);
        auditLogRepository.save(AuditLog.builder().actorUserId(adminUserId).action("CLINIC_REJECT").details("Clinic " + id + " rejected: " + reason).build());
        java.time.Instant rejectionTime = saved.getApprovedAt();
        var body = new com.passmais.interfaces.dto.RejectClinicResponseDTO(
                "Clínica rejeitada com sucesso",
                new com.passmais.interfaces.dto.RejectClinicResponseDTO.Clinic(saved.getId(), rejectionTime)
        );
        return ResponseEntity.ok(body);
    }
}

package com.passmais.interfaces.controller;

import com.passmais.application.service.AdminApprovalService;
import com.passmais.domain.entity.Clinic;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.AuditLog;
import com.passmais.infrastructure.repository.AuditLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminApprovalService adminApprovalService;
    private final AuditLogRepository auditLogRepository;

    public AdminController(AdminApprovalService adminApprovalService, AuditLogRepository auditLogRepository) {
        this.adminApprovalService = adminApprovalService;
        this.auditLogRepository = auditLogRepository;
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
    public ResponseEntity<com.passmais.interfaces.dto.RejectDoctorResponseDTO> rejectDoctor(@PathVariable("id") UUID id, @RequestParam(name = "adminUserId") UUID adminUserId, @RequestParam(name = "reason", required = false) String reason) {
        adminApprovalService.rejectDoctor(id);
        auditLogRepository.save(AuditLog.builder().actorUserId(adminUserId).action("DOCTOR_REJECT").details("Doctor " + id + " rejected: " + reason).build());
        java.time.Instant now = java.time.Instant.now();
        var body = new com.passmais.interfaces.dto.RejectDoctorResponseDTO(
                "Médico rejeitado com sucesso",
                new com.passmais.interfaces.dto.RejectDoctorResponseDTO.Doctor(id, now)
        );
        return ResponseEntity.ok(body);
    }

    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @PostMapping("/reject/clinic/{id}")
    public ResponseEntity<com.passmais.interfaces.dto.RejectClinicResponseDTO> rejectClinic(@PathVariable("id") UUID id, @RequestParam(name = "adminUserId") UUID adminUserId, @RequestParam(name = "reason", required = false) String reason) {
        adminApprovalService.rejectClinic(id);
        auditLogRepository.save(AuditLog.builder().actorUserId(adminUserId).action("CLINIC_REJECT").details("Clinic " + id + " rejected: " + reason).build());
        java.time.Instant now = java.time.Instant.now();
        var body = new com.passmais.interfaces.dto.RejectClinicResponseDTO(
                "Clínica rejeitada com sucesso",
                new com.passmais.interfaces.dto.RejectClinicResponseDTO.Clinic(id, now)
        );
        return ResponseEntity.ok(body);
    }
}

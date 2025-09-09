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

    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @PostMapping("/approve/doctor/{id}")
    public ResponseEntity<DoctorProfile> approveDoctor(@PathVariable UUID id) {
        return ResponseEntity.ok(adminApprovalService.approveDoctor(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @PostMapping("/approve/clinic/{id}")
    public ResponseEntity<Clinic> approveClinic(@PathVariable UUID id) {
        return ResponseEntity.ok(adminApprovalService.approveClinic(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @PostMapping("/reject/doctor/{id}")
    public ResponseEntity<DoctorProfile> rejectDoctor(@PathVariable UUID id, @RequestParam UUID adminUserId, @RequestParam(required = false) String reason) {
        DoctorProfile d = adminApprovalService.rejectDoctor(id);
        auditLogRepository.save(AuditLog.builder().actorUserId(adminUserId).action("DOCTOR_REJECT").details("Doctor " + id + " rejected: " + reason).build());
        return ResponseEntity.ok(d);
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @PostMapping("/reject/clinic/{id}")
    public ResponseEntity<Clinic> rejectClinic(@PathVariable UUID id, @RequestParam UUID adminUserId, @RequestParam(required = false) String reason) {
        Clinic c = adminApprovalService.rejectClinic(id);
        auditLogRepository.save(AuditLog.builder().actorUserId(adminUserId).action("CLINIC_REJECT").details("Clinic " + id + " rejected: " + reason).build());
        return ResponseEntity.ok(c);
    }
}

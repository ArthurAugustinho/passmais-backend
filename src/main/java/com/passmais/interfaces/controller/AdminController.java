package com.passmais.interfaces.controller;

import com.passmais.application.service.AdminApprovalService;
import com.passmais.domain.entity.Clinic;
import com.passmais.domain.entity.DoctorProfile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminApprovalService adminApprovalService;

    public AdminController(AdminApprovalService adminApprovalService) {
        this.adminApprovalService = adminApprovalService;
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
}


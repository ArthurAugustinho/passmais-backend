package com.passmais.interfaces.controller;

import com.passmais.domain.entity.DoctorProfile;
import com.passmais.interfaces.dto.DoctorProfileAdminDTO;
import com.passmais.infrastructure.repository.DoctorProfileRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/doctor-profiles")
public class AdminDoctorProfilesController {

    private final DoctorProfileRepository doctorProfileRepository;

    public AdminDoctorProfilesController(DoctorProfileRepository doctorProfileRepository) {
        this.doctorProfileRepository = doctorProfileRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<List<DoctorProfileAdminDTO>> listAll() {
        List<DoctorProfileAdminDTO> dtos = doctorProfileRepository.findAll().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    private DoctorProfileAdminDTO toDto(DoctorProfile p) {
        return new DoctorProfileAdminDTO(
                p.getId(),
                p.getUser() != null ? p.getUser().getName() : null,
                p.getUser() != null ? p.getUser().getEmail() : null,
                p.getUser() != null ? p.getUser().getRole() : null,
                p.getCrm(),
                p.getSpecialty(),
                p.getBio(),
                p.getPhone(),
                p.getCpf(),
                p.getBirthDate(),
                p.getPhotoUrl(),
                p.getConsultationPrice(),
                p.isApproved(),
                p.getApprovedAt(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}

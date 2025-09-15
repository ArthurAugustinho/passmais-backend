package com.passmais.interfaces.controller;

import com.passmais.domain.entity.Clinic;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.infrastructure.repository.ClinicRepository;
import com.passmais.infrastructure.repository.DoctorProfileRepository;
import com.passmais.interfaces.dto.PendingClinicDTO;
import com.passmais.interfaces.dto.PendingDoctorDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/pending")
public class AdminPendingController {

    private final DoctorProfileRepository doctorRepo;
    private final ClinicRepository clinicRepo;

    public AdminPendingController(DoctorProfileRepository doctorRepo, ClinicRepository clinicRepo) {
        this.doctorRepo = doctorRepo;
        this.clinicRepo = clinicRepo;
    }

    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping("/doctors")
    public ResponseEntity<List<PendingDoctorDTO>> listPendingDoctors() {
        List<DoctorProfile> list = doctorRepo.findByApprovedAtIsNull();
        List<PendingDoctorDTO> resp = list.stream().map(d -> new PendingDoctorDTO(
                d.getId(),
                d.getUser().getName(),
                d.getSpecialty(),
                d.getCrm(),
                d.getCreatedAt(),
                d.getApprovedAt()
        )).toList();
        return ResponseEntity.ok(resp);
    }

    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping("/clinics")
    public ResponseEntity<List<PendingClinicDTO>> listPendingClinics() {
        List<Clinic> list = clinicRepo.findByApprovedAtIsNull();
        List<PendingClinicDTO> resp = list.stream().map(c -> new PendingClinicDTO(
                c.getId(),
                c.getName(),
                c.getCreatedAt(),
                c.getApprovedAt()
        )).toList();
        return ResponseEntity.ok(resp);
    }
}

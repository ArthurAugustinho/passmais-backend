package com.passmais.interfaces.controller;

import com.passmais.domain.entity.PatientProfile;
import com.passmais.infrastructure.repository.PatientProfileRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientProfileRepository patientRepo;

    public PatientController(PatientProfileRepository patientRepo) {
        this.patientRepo = patientRepo;
    }

    @PreAuthorize("hasRole('PATIENT')")
    @GetMapping("/{id}")
    public ResponseEntity<PatientProfile> get(@PathVariable UUID id) {
        return ResponseEntity.ok(patientRepo.findById(id).orElseThrow());
    }

    @PreAuthorize("hasRole('PATIENT')")
    @PutMapping("/{id}")
    public ResponseEntity<PatientProfile> update(@PathVariable UUID id,
                                                 @RequestParam(name = "cpf", required = false) String cpf,
                                                 @RequestParam(name = "birthDate", required = false) String birthDate,
                                                 @RequestParam(name = "address", required = false) String address,
                                                 @RequestParam(name = "cellPhone", required = false) String cellPhone,
                                                 @RequestParam(name = "communicationPreference", required = false) String communicationPreference) {
        PatientProfile p = patientRepo.findById(id).orElseThrow();
        if (cpf != null) p.setCpf(cpf);
        if (birthDate != null) p.setBirthDate(LocalDate.parse(birthDate));
        if (address != null) p.setAddress(address);
        if (cellPhone != null) p.setCellPhone(cellPhone);
        if (communicationPreference != null) p.setCommunicationPreference(communicationPreference);
        return ResponseEntity.ok(patientRepo.save(p));
    }
}


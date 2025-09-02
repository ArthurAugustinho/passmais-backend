package com.passmais.interfaces.controller;

import com.passmais.application.service.AvailabilityService;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.infrastructure.repository.DoctorProfileRepository;
import com.passmais.interfaces.dto.AvailabilityCreateDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/availabilities")
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final DoctorProfileRepository doctorRepo;

    public AvailabilityController(AvailabilityService availabilityService, DoctorProfileRepository doctorRepo) {
        this.availabilityService = availabilityService;
        this.doctorRepo = doctorRepo;
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping("/doctor/{doctorId}")
    public ResponseEntity<?> create(@PathVariable UUID doctorId, @RequestBody @Valid AvailabilityCreateDTO dto) {
        DoctorProfile doctor = doctorRepo.findById(doctorId).orElseThrow();
        return ResponseEntity.ok(availabilityService.createAvailability(doctor, dto.dayOfWeek(), dto.startTime(), dto.endTime()));
    }
}


package com.passmais.interfaces.controller;

import com.passmais.application.service.AvailabilityService;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.infrastructure.repository.DoctorProfileRepository;
import com.passmais.infrastructure.repository.AvailabilityRepository;
import com.passmais.interfaces.dto.AvailabilityCreateDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/availabilities")
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final DoctorProfileRepository doctorRepo;
    private final AvailabilityRepository availabilityRepository;

    public AvailabilityController(AvailabilityService availabilityService, DoctorProfileRepository doctorRepo, AvailabilityRepository availabilityRepository) {
        this.availabilityService = availabilityService;
        this.doctorRepo = doctorRepo;
        this.availabilityRepository = availabilityRepository;
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping("/doctor/{doctorId}")
    public ResponseEntity<?> create(@PathVariable UUID doctorId, @RequestBody @Valid AvailabilityCreateDTO dto) {
        DoctorProfile doctor = doctorRepo.findById(doctorId).orElseThrow();
        return ResponseEntity.ok(availabilityService.createAvailability(doctor, dto.dayOfWeek(), dto.startTime(), dto.endTime()));
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @GetMapping("/doctor/{doctorId}/day/{day}")
    public ResponseEntity<?> listByDay(@PathVariable UUID doctorId, @PathVariable("day") String day) {
        DoctorProfile doctor = doctorRepo.findById(doctorId).orElseThrow();
        DayOfWeek dow = DayOfWeek.valueOf(day.toUpperCase());
        return ResponseEntity.ok(availabilityService.listByDay(doctor, dow));
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        availabilityRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

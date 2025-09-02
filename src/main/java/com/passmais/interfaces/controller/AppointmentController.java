package com.passmais.interfaces.controller;

import com.passmais.application.service.AppointmentService;
import com.passmais.domain.entity.Appointment;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.PatientProfile;
import com.passmais.infrastructure.repository.AppointmentRepository;
import com.passmais.infrastructure.repository.DoctorProfileRepository;
import com.passmais.infrastructure.repository.PatientProfileRepository;
import com.passmais.interfaces.dto.AppointmentCreateDTO;
import com.passmais.interfaces.dto.AppointmentResponseDTO;
import com.passmais.interfaces.mapper.AppointmentMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentRepository appointmentRepository;
    private final DoctorProfileRepository doctorRepo;
    private final PatientProfileRepository patientRepo;
    private final AppointmentMapper appointmentMapper;

    public AppointmentController(AppointmentService appointmentService,
                                 AppointmentRepository appointmentRepository,
                                 DoctorProfileRepository doctorRepo,
                                 PatientProfileRepository patientRepo,
                                 AppointmentMapper appointmentMapper) {
        this.appointmentService = appointmentService;
        this.appointmentRepository = appointmentRepository;
        this.doctorRepo = doctorRepo;
        this.patientRepo = patientRepo;
        this.appointmentMapper = appointmentMapper;
    }

    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping
    public ResponseEntity<AppointmentResponseDTO> schedule(@RequestBody @Valid AppointmentCreateDTO dto) {
        DoctorProfile doctor = doctorRepo.findById(dto.doctorId()).orElseThrow();
        PatientProfile patient = patientRepo.findById(dto.patientId()).orElseThrow();
        Appointment appt = appointmentService.schedule(doctor, patient, dto.dateTime());
        return ResponseEntity.ok(appointmentMapper.toResponse(appt));
    }

    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping("/{id}/reschedule")
    public ResponseEntity<AppointmentResponseDTO> reschedule(@PathVariable UUID id, @RequestParam("dateTime") String dateTimeIso) {
        Appointment original = appointmentRepository.findById(id).orElseThrow();
        java.time.Instant newDateTime = java.time.Instant.parse(dateTimeIso);
        Appointment updated = appointmentService.reschedule(original, newDateTime);
        return ResponseEntity.ok(appointmentMapper.toResponse(updated));
    }

    @PreAuthorize("hasAnyRole('PATIENT','ADMIN','SUPERADMIN')")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponseDTO> cancel(@PathVariable UUID id) {
        Appointment appt = appointmentRepository.findById(id).orElseThrow();
        Appointment canceled = appointmentService.cancel(appt);
        return ResponseEntity.ok(appointmentMapper.toResponse(canceled));
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping("/{id}/done")
    public ResponseEntity<AppointmentResponseDTO> markDone(@PathVariable UUID id) {
        Appointment appt = appointmentRepository.findById(id).orElseThrow();
        Appointment done = appointmentService.markDone(appt);
        return ResponseEntity.ok(appointmentMapper.toResponse(done));
    }
}


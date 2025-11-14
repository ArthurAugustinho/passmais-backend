package com.passmais.interfaces.controller;

import com.passmais.domain.entity.Appointment;
import com.passmais.domain.entity.ClinicalNote;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.User;
import com.passmais.domain.enums.Role;
import com.passmais.infrastructure.repository.AppointmentRepository;
import com.passmais.infrastructure.repository.ClinicalNoteRepository;
import com.passmais.infrastructure.repository.DoctorProfileRepository;
import com.passmais.infrastructure.repository.UserRepository;
import com.passmais.interfaces.dto.ClinicalNoteCreateDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clinical-notes")
public class ClinicalNoteController {

    private final ClinicalNoteRepository clinicalNoteRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final UserRepository userRepository;

    public ClinicalNoteController(ClinicalNoteRepository clinicalNoteRepository,
                                  AppointmentRepository appointmentRepository,
                                  DoctorProfileRepository doctorProfileRepository,
                                  UserRepository userRepository) {
        this.clinicalNoteRepository = clinicalNoteRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping
    public ResponseEntity<ClinicalNote> create(@RequestBody @Valid ClinicalNoteCreateDTO dto) {
        Appointment appt = appointmentRepository.findById(dto.appointmentId()).orElseThrow();
        DoctorProfile doctor = doctorProfileRepository.findById(dto.doctorId()).orElseThrow();
        ClinicalNote note = ClinicalNote.builder()
                .appointment(appt)
                .doctor(doctor)
                .notes(dto.notes())
                .build();
        return ResponseEntity.ok(clinicalNoteRepository.save(note));
    }

    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN','SUPERADMIN')")
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<ClinicalNote>> listByPatient(@PathVariable UUID patientId) {
        User patientUser = userRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        if (patientUser.getRole() != Role.PATIENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário informado não possui papel de paciente");
        }
        // Notas por paciente via consultas
        List<ClinicalNote> notes = clinicalNoteRepository.findAll().stream()
                .filter(n -> n.getAppointment().getPatient().getId().equals(patientUser.getId()))
                .toList();
        return ResponseEntity.ok(notes);
    }
}

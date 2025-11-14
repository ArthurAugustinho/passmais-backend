package com.passmais.interfaces.controller;

import com.passmais.application.service.AppointmentService;
import com.passmais.domain.entity.Appointment;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.PatientProfile;
import com.passmais.domain.entity.User;
import com.passmais.domain.enums.Role;
import com.passmais.infrastructure.repository.AppointmentRepository;
import com.passmais.infrastructure.repository.DoctorProfileRepository;
import com.passmais.infrastructure.repository.PatientProfileRepository;
import com.passmais.infrastructure.repository.UserRepository;
import com.passmais.interfaces.dto.*;
import com.passmais.interfaces.mapper.AppointmentMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentRepository appointmentRepository;
    private final DoctorProfileRepository doctorRepo;
    private final PatientProfileRepository patientRepo;
    private final UserRepository userRepository;
    private final AppointmentMapper appointmentMapper;

    public AppointmentController(AppointmentService appointmentService,
                                 AppointmentRepository appointmentRepository,
                                 DoctorProfileRepository doctorRepo,
                                 PatientProfileRepository patientRepo,
                                 UserRepository userRepository,
                                 AppointmentMapper appointmentMapper) {
        this.appointmentService = appointmentService;
        this.appointmentRepository = appointmentRepository;
        this.doctorRepo = doctorRepo;
        this.patientRepo = patientRepo;
        this.userRepository = userRepository;
        this.appointmentMapper = appointmentMapper;
    }

    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping
    public ResponseEntity<AppointmentConfirmationResponseDTO> schedule(@RequestBody @Valid AppointmentCreateDTO dto) {
        DoctorProfile doctor = doctorRepo.findById(dto.doctorId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Médico não encontrado"));
        User patientUser = userRepository.findById(dto.patientId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado"));
        if (patientUser.getRole() != Role.PATIENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário informado não possui papel de paciente");
        }
        PatientProfile patientProfile = patientRepo.findByUserId(patientUser.getId()).orElse(null);
        Appointment appt = appointmentService.schedule(
                doctor,
                patientUser,
                patientProfile,
                dto.appointmentDateTime(),
                dto.bookingDateTime(),
                dto.reason(),
                dto.consultationValue(),
                dto.location(),
                dto.patientFullName(),
                dto.patientCpf(),
                dto.patientBirthDate(),
                dto.patientCellPhone()
        );
        AppointmentConfirmationResponseDTO response = new AppointmentConfirmationResponseDTO(
                "Consulta agendada com sucesso",
                appointmentMapper.toResponse(appt)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    @PostMapping("/{id}/reschedule")
    public ResponseEntity<AppointmentResponseDTO> reschedule(@PathVariable UUID id, @RequestParam("dateTime") String dateTimeIso) {
        Appointment original = appointmentRepository.findById(id).orElseThrow();
        java.time.Instant newDateTime = java.time.Instant.parse(dateTimeIso);
        Appointment updated = appointmentService.reschedule(original, newDateTime);
        return ResponseEntity.ok(appointmentMapper.toResponse(updated));
    }

    @PreAuthorize("hasAnyRole('PATIENT','ADMINISTRATOR')")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponseDTO> cancel(@PathVariable UUID id) {
        Appointment appt = appointmentRepository.findById(id).orElseThrow();
        Appointment canceled = appointmentService.cancel(appt, null);
        return ResponseEntity.ok(appointmentMapper.toResponse(canceled));
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping("/{id}/done")
    public ResponseEntity<AppointmentResponseDTO> markDone(@PathVariable UUID id) {
        Appointment appt = appointmentRepository.findById(id).orElseThrow();
        Appointment done = appointmentService.markDone(appt);
        return ResponseEntity.ok(appointmentMapper.toResponse(done));
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping("/{id}/start")
    public ResponseEntity<AppointmentResponseDTO> start(@PathVariable UUID id) {
        Appointment appt = appointmentRepository.findById(id).orElseThrow();
        appt.setStatus(com.passmais.domain.enums.AppointmentStatus.IN_PROGRESS);
        return ResponseEntity.ok(appointmentMapper.toResponse(appointmentRepository.save(appt)));
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping("/{id}/observations")
    public ResponseEntity<AppointmentResponseDTO> addObservation(@PathVariable UUID id, @RequestBody @Valid AppointmentObservationDTO dto) {
        Appointment appt = appointmentRepository.findById(id).orElseThrow();
        appt.setObservations(dto.text());
        return ResponseEntity.ok(appointmentMapper.toResponse(appointmentRepository.save(appt)));
    }

    @PreAuthorize("hasAnyRole('DOCTOR','ADMINISTRATOR','PATIENT')")
    @PostMapping("/{id}/cancel-with-reason")
    public ResponseEntity<AppointmentResponseDTO> cancelWithReason(@PathVariable UUID id, @RequestBody @Valid AppointmentCancelDTO dto) {
        Appointment appt = appointmentRepository.findById(id).orElseThrow();
        appt.setObservations(dto.reason());
        Appointment canceled = appointmentService.cancel(appt, dto.reason());
        return ResponseEntity.ok(appointmentMapper.toResponse(canceled));
    }

    

    @PreAuthorize("hasRole('DOCTOR')")
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<AppointmentResponseDTO>> listForDoctor(@PathVariable UUID doctorId, @RequestParam(name = "status", required = false) com.passmais.domain.enums.AppointmentStatus status) {
        DoctorProfile doctor = doctorRepo.findById(doctorId).orElseThrow();
        List<Appointment> list = status == null ? appointmentRepository.findByDoctor(doctor) : appointmentRepository.findByDoctorAndStatus(doctor, status);
        return ResponseEntity.ok(list.stream().map(appointmentMapper::toResponse).toList());
    }

    @PreAuthorize("hasAnyRole('PATIENT','ADMINISTRATOR')")
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<AppointmentResponseDTO>> listForPatient(@PathVariable UUID patientId, @RequestParam(name = "status", required = false) com.passmais.domain.enums.AppointmentStatus status) {
        User patientUser = userRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        if (patientUser.getRole() != Role.PATIENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário informado não possui papel de paciente");
        }
        List<Appointment> list = status == null
                ? appointmentRepository.findByPatient(patientUser)
                : appointmentRepository.findByPatientAndStatus(patientUser, status);
        return ResponseEntity.ok(list.stream().map(appointmentMapper::toResponse).toList());
    }
}

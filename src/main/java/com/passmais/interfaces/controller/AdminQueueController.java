package com.passmais.interfaces.controller;

import com.passmais.domain.entity.Appointment;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.AuditLog;
import com.passmais.domain.enums.AppointmentStatus;
import com.passmais.infrastructure.repository.AppointmentRepository;
import com.passmais.infrastructure.repository.DoctorProfileRepository;
import com.passmais.infrastructure.repository.AuditLogRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/queue")
public class AdminQueueController {

    private final AppointmentRepository appointmentRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminQueueController(AppointmentRepository appointmentRepository, DoctorProfileRepository doctorProfileRepository, AuditLogRepository auditLogRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping("/doctor/{doctorId}/today")
    public ResponseEntity<List<Appointment>> listTodayQueue(@PathVariable UUID doctorId) {
        DoctorProfile doctor = doctorProfileRepository.findById(doctorId).orElseThrow();
        Instant start = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = LocalDate.now().atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
        List<Appointment> list = appointmentRepository.findByDoctorAndDateTimeBetween(doctor, start, end)
                .stream()
                .filter(a -> a.getStatus() == AppointmentStatus.PENDING
                        || a.getStatus() == AppointmentStatus.CONFIRMED
                        || a.getStatus() == AppointmentStatus.IN_PROGRESS)
                .sorted(java.util.Comparator.comparing(Appointment::getDateTime))
                .toList();
        return ResponseEntity.ok(list);
    }

    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @PostMapping("/reorder")
    public ResponseEntity<Appointment> reorder(
            @RequestParam UUID appointmentId,
            @RequestParam @NotNull String newDateTimeIso,
            @RequestParam @NotNull UUID adminUserId) {
        Appointment appt = appointmentRepository.findById(appointmentId).orElseThrow();
        Instant newDt = Instant.parse(newDateTimeIso);
        Instant oldDt = appt.getDateTime();
        appt.setDateTime(newDt);
        Appointment saved = appointmentRepository.save(appt);
        auditLogRepository.save(AuditLog.builder()
                .actorUserId(adminUserId)
                .action("QUEUE_REORDER")
                .details("Appointment " + appointmentId + " from " + oldDt + " to " + newDt)
                .build());
        return ResponseEntity.ok(saved);
    }

    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @PostMapping("/remove/{appointmentId}")
    public ResponseEntity<Map<String, Object>> remove(@PathVariable UUID appointmentId, @RequestParam @NotNull UUID adminUserId, @RequestParam(required = false) String reason) {
        Appointment appt = appointmentRepository.findById(appointmentId).orElseThrow();
        appt.setStatus(AppointmentStatus.CANCELED);
        if (reason != null) {
            appt.setObservations(reason);
            appt.setCanceledReason(reason);
        } else {
            appt.setCanceledReason(null);
        }
        appt.setCanceledAt(Instant.now());
        appointmentRepository.save(appt);
        auditLogRepository.save(AuditLog.builder()
                .actorUserId(adminUserId)
                .action("QUEUE_REMOVE")
                .details("Appointment " + appointmentId + " canceled. Reason: " + reason)
                .build());
        return ResponseEntity.ok(Map.of("status", "removed"));
    }
}

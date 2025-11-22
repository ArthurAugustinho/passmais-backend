package com.passmais.interfaces.controller;

import com.passmais.domain.entity.Appointment;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.enums.AppointmentStatus;
import com.passmais.infrastructure.repository.AppointmentRepository;
import com.passmais.infrastructure.repository.DoctorProfileRepository;
import com.passmais.interfaces.dto.DoctorDashboardDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctor/dashboard")
public class DoctorDashboardController {

    private final AppointmentRepository appointmentRepository;
    private final DoctorProfileRepository doctorProfileRepository;

    public DoctorDashboardController(AppointmentRepository appointmentRepository, DoctorProfileRepository doctorProfileRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorProfileRepository = doctorProfileRepository;
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @GetMapping("/{doctorId}")
    public ResponseEntity<DoctorDashboardDTO> overview(@PathVariable UUID doctorId) {
        DoctorProfile doctor = doctorProfileRepository.findById(doctorId).orElseThrow();
        var start = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC);
        var end = LocalDate.now().atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
        List<Appointment> today = appointmentRepository.findByDoctorAndDateTimeBetween(doctor, start, end);

        long total = today.size();
        long pend = today.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.PENDING || a.getStatus() == AppointmentStatus.CONFIRMED)
                .count();
        long done = today.stream().filter(a -> a.getStatus() == AppointmentStatus.DONE).count();
        long canc = today.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELED).count();

        List<DoctorDashboardDTO.SimpleAppointmentItem> ultimos = today.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.DONE)
                .sorted(Comparator.comparing(Appointment::getDateTime).reversed())
                .limit(5)
                .map(a -> new DoctorDashboardDTO.SimpleAppointmentItem(a.getId(), a.getDateTime(), a.getPatient().getId()))
                .toList();

        List<DoctorDashboardDTO.SimpleAppointmentItem> proximos = today.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.PENDING
                        || a.getStatus() == AppointmentStatus.CONFIRMED
                        || a.getStatus() == AppointmentStatus.IN_PROGRESS)
                .sorted(Comparator.comparing(Appointment::getDateTime))
                .limit(5)
                .map(a -> new DoctorDashboardDTO.SimpleAppointmentItem(a.getId(), a.getDateTime(), a.getPatient().getId()))
                .toList();

        DoctorDashboardDTO dto = new DoctorDashboardDTO(total, pend, done, canc, ultimos, proximos);
        return ResponseEntity.ok(dto);
    }
}

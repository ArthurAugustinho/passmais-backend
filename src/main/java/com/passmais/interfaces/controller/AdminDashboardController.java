package com.passmais.interfaces.controller;

import com.passmais.domain.enums.Role;
import com.passmais.infrastructure.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final AppointmentRepository appointmentRepository;
    private final ReviewRepository reviewRepository;
    private final ClinicRepository clinicRepository;

    public AdminDashboardController(UserRepository userRepository,
                                    DoctorProfileRepository doctorProfileRepository,
                                    AppointmentRepository appointmentRepository,
                                    ReviewRepository reviewRepository,
                                    ClinicRepository clinicRepository) {
        this.userRepository = userRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.appointmentRepository = appointmentRepository;
        this.reviewRepository = reviewRepository;
        this.clinicRepository = clinicRepository;
    }

    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping
    public ResponseEntity<Map<String, Object>> overview() {
        long totalPatients = userRepository.findAll().stream().filter(u -> u.getRole() == Role.PATIENT).count();
        long totalDoctors = doctorProfileRepository.count();
        long totalClinics = clinicRepository.count();
        long totalAppointments = appointmentRepository.count();
        long totalReviews = reviewRepository.count();

        Map<String, Object> resp = Map.of(
                "totalPatients", totalPatients,
                "totalDoctors", totalDoctors,
                "totalClinics", totalClinics,
                "totalAppointments", totalAppointments,
                "totalReviews", totalReviews
        );
        return ResponseEntity.ok(resp);
    }
}

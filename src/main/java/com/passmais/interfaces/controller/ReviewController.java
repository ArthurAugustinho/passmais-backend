package com.passmais.interfaces.controller;

import com.passmais.application.service.ReviewService;
import com.passmais.domain.entity.Appointment;
import com.passmais.domain.entity.Review;
import com.passmais.infrastructure.repository.AppointmentRepository;
import com.passmais.interfaces.dto.ReviewCreateDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final AppointmentRepository appointmentRepository;

    public ReviewController(ReviewService reviewService, AppointmentRepository appointmentRepository) {
        this.reviewService = reviewService;
        this.appointmentRepository = appointmentRepository;
    }

    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping
    public ResponseEntity<Review> create(@RequestBody @Valid ReviewCreateDTO dto) {
        Appointment appt = appointmentRepository.findById(dto.appointmentId()).orElseThrow();
        Review review = reviewService.createFromAppointment(appt, dto.rating(), dto.comment());
        return ResponseEntity.ok(review);
    }
}


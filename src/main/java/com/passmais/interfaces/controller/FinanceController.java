package com.passmais.interfaces.controller;

import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.Payment;
import com.passmais.domain.enums.PaymentStatus;
import com.passmais.infrastructure.repository.DoctorProfileRepository;
import com.passmais.infrastructure.repository.PaymentRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    private final PaymentRepository paymentRepository;
    private final DoctorProfileRepository doctorProfileRepository;

    public FinanceController(PaymentRepository paymentRepository, DoctorProfileRepository doctorProfileRepository) {
        this.paymentRepository = paymentRepository;
        this.doctorProfileRepository = doctorProfileRepository;
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @GetMapping("/doctor/{doctorId}/payments")
    public ResponseEntity<List<Payment>> listPayments(@PathVariable UUID doctorId, @RequestParam(name = "status", required = false) PaymentStatus status) {
        DoctorProfile doctor = doctorProfileRepository.findById(doctorId).orElseThrow();
        return ResponseEntity.ok(paymentRepository.findByDoctorAndStatus(doctor, status));
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @GetMapping("/doctor/{doctorId}/balance")
    public ResponseEntity<BigDecimal> balance(@PathVariable UUID doctorId) {
        DoctorProfile doctor = doctorProfileRepository.findById(doctorId).orElseThrow();
        return ResponseEntity.ok(paymentRepository.sumPendingByDoctor(doctor));
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @GetMapping("/doctor/{doctorId}/export")
    public ResponseEntity<byte[]> exportCsv(@PathVariable UUID doctorId) {
        DoctorProfile doctor = doctorProfileRepository.findById(doctorId).orElseThrow();
        List<Payment> list = paymentRepository.findByDoctorAndStatus(doctor, null);
        StringBuilder sb = new StringBuilder();
        sb.append("payment_id,appointment_id,date,value,status\n");
        for (Payment p : list) {
            sb.append(p.getId()).append(',')
              .append(p.getAppointment().getId()).append(',')
              .append(p.getPaidAt() != null ? p.getPaidAt() : "").append(',')
              .append(p.getValue()).append(',')
              .append(p.getStatus()).append('\n');
        }
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=financeiro.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(bytes);
    }
}


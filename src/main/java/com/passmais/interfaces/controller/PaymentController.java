package com.passmais.interfaces.controller;

import com.passmais.domain.entity.Appointment;
import com.passmais.domain.entity.Payment;
import com.passmais.domain.enums.PaymentMethod;
import com.passmais.domain.enums.PaymentStatus;
import com.passmais.infrastructure.repository.AppointmentRepository;
import com.passmais.infrastructure.repository.PaymentRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;

    public PaymentController(PaymentRepository paymentRepository, AppointmentRepository appointmentRepository) {
        this.paymentRepository = paymentRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping("/appointment/{appointmentId}")
    public ResponseEntity<Payment> createOrConfirm(
            @PathVariable UUID appointmentId,
            @RequestParam @NotNull PaymentMethod method) {
        Appointment appt = appointmentRepository.findById(appointmentId).orElseThrow();
        Payment p = paymentRepository.findAll().stream()
                .filter(x -> x.getAppointment().getId().equals(appt.getId()))
                .findFirst()
                .orElse(Payment.builder().appointment(appt).build());
        p.setMethod(method);
        if (p.getStatus() != PaymentStatus.PAID) {
            // Simulação de confirmação imediata
            p.setStatus(PaymentStatus.PAID);
            p.setPaidAt(Instant.now());
            p.setReceipt("REC-" + java.util.UUID.randomUUID());
        }
        return ResponseEntity.ok(paymentRepository.save(p));
    }

    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN','SUPERADMIN')")
    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> get(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentRepository.findById(paymentId).orElseThrow());
    }

    @PreAuthorize("hasRole('PATIENT')")
    @GetMapping("/{paymentId}/receipt")
    public ResponseEntity<byte[]> receipt(@PathVariable UUID paymentId) {
        Payment p = paymentRepository.findById(paymentId).orElseThrow();
        String content = "Comprovante de Pagamento\n" +
                "Pagamento: " + p.getId() + "\n" +
                "Consulta: " + p.getAppointment().getId() + "\n" +
                "Método: " + p.getMethod() + "\n" +
                "Status: " + p.getStatus() + "\n" +
                "Pago em: " + (p.getPaidAt() != null ? p.getPaidAt() : "-") + "\n" +
                "Recibo: " + (p.getReceipt() != null ? p.getReceipt() : "-") + "\n";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt-" + paymentId + ".txt")
                .contentType(MediaType.TEXT_PLAIN)
                .body(bytes);
    }
}


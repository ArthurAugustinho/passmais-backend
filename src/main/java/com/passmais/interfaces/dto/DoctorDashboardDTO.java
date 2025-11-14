package com.passmais.interfaces.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DoctorDashboardDTO(
        long totalHoje,
        long pendentes,
        long realizadas,
        long canceladas,
        List<SimpleAppointmentItem> ultimosAtendimentos,
        List<SimpleAppointmentItem> proximosAgendamentos
) {
    public record SimpleAppointmentItem(UUID id, Instant dateTime, UUID patientId) {}
}


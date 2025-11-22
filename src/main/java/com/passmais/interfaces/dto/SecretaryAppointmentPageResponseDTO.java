package com.passmais.interfaces.dto;

import java.util.List;

public record SecretaryAppointmentPageResponseDTO(
        List<SecretaryAppointmentResponseDTO> items,
        int page,
        int pageSize,
        long totalElements
) {}


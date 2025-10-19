package com.passmais.interfaces.dto.consultation;

import java.util.List;

public record DoctorConsultationPageResponseDTO(
        List<DoctorConsultationListItemDTO> items,
        int page,
        int pageSize,
        long total
) {}


package com.passmais.interfaces.dto.schedule;

import java.util.List;

public record PatientScheduleDayResponse(
        String isoDate,
        String label,
        String source,
        List<String> slots,
        boolean blocked
) {
}

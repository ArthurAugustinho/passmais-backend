package com.passmais.interfaces.dto.schedule;

import java.util.List;

public record ScheduleBatchDayRequest(
        String isoDate,
        String label,
        String source,
        List<String> slots
) {
}

package com.passmais.interfaces.dto.schedule;

import java.util.Map;

public record ScheduleErrorResponse(
        String status,
        String code,
        String message,
        Map<String, Object> details
) {
}

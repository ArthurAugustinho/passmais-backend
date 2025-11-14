package com.passmais.interfaces.dto.schedule;

import java.time.LocalDate;
import java.util.List;

public record RecurringScheduleSettingsPayload(
        Integer appointmentInterval,
        Integer bufferMinutes,
        LocalDate startDate,
        LocalDate endDate,
        Boolean noEndDate,
        List<LocalDate> exceptions
) {
}

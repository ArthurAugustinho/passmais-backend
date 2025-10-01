package com.passmais.interfaces.dto.schedule;

public record SpecificScheduleSettingsPayload(
        Integer appointmentInterval,
        Integer bufferMinutes
) {
}

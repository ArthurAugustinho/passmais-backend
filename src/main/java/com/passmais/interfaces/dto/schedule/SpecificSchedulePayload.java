package com.passmais.interfaces.dto.schedule;

import java.util.Map;

public record SpecificSchedulePayload(
        SpecificScheduleSettingsPayload settings,
        Map<String, SpecificDayPayload> days
) {
}

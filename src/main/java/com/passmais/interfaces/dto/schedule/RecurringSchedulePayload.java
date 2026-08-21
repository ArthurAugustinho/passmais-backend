package com.passmais.interfaces.dto.schedule;

import java.util.Map;

public record RecurringSchedulePayload(
        RecurringScheduleSettingsPayload settings,
        Map<String, RecurringDayPayload> schedule
) {
}

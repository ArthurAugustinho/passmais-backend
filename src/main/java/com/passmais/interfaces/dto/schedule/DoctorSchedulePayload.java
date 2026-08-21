package com.passmais.interfaces.dto.schedule;

import com.passmais.domain.enums.ScheduleMode;

public record DoctorSchedulePayload(
        ScheduleMode mode,
        SpecificSchedulePayload specific,
        RecurringSchedulePayload recurring,
        RecurringGlobalSettingsPayload recurringSettings
) {
}

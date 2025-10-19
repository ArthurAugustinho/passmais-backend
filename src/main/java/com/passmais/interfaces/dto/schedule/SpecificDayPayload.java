package com.passmais.interfaces.dto.schedule;

import java.util.List;

public record SpecificDayPayload(
        List<ScheduleSlotPayload> slots
) {
}

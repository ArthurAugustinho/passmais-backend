package com.passmais.interfaces.dto.schedule;

import java.util.List;

public record ScheduleBatchUpsertResponse(
        String doctorId,
        String timezone,
        int receivedDays,
        int createdSlots,
        int blockedDays,
        boolean replacedPreviousVersions,
        List<ScheduleBatchDayResponse> days
) {
}

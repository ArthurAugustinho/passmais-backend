package com.passmais.interfaces.dto.schedule;

public record ScheduleBatchDayResponse(
        String isoDate,
        String source,
        int slotsCreated,
        boolean previousVersionSoftDeleted,
        Boolean blocked
) {
}

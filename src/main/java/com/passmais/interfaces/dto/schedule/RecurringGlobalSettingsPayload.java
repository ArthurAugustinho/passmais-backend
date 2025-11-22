package com.passmais.interfaces.dto.schedule;

public record RecurringGlobalSettingsPayload(
        Boolean enabled,
        Boolean isRecurringActive
) {
}

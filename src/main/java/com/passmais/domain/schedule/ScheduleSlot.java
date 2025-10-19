package com.passmais.domain.schedule;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScheduleSlot(
        String id,
        LocalTime start,
        LocalTime end,
        Integer intervalMinutes,
        Integer endBufferMinutes
) {
}

package com.passmais.interfaces.dto.schedule;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record ScheduleExceptionRequest(
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate exceptionDate,
        String description,
        Boolean remove
) {
}

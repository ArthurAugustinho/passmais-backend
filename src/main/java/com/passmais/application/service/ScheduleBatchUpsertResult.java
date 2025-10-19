package com.passmais.application.service;

import com.passmais.interfaces.dto.schedule.ScheduleBatchUpsertResponse;
import org.springframework.http.HttpStatus;

public record ScheduleBatchUpsertResult(
        ScheduleBatchUpsertResponse body,
        HttpStatus status
) {
}

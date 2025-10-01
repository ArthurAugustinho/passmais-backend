package com.passmais.application.service.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class ScheduleException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, Object> details;

    public ScheduleException(HttpStatus status, String code, String message) {
        this(status, code, message, Map.of());
    }

    public ScheduleException(HttpStatus status, String code, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}

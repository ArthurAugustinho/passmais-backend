package com.passmais.domain.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.Objects;

public class ApiErrorException extends RuntimeException {
    private final String code;
    private final HttpStatus status;
    private final Map<String, String> fieldErrors;

    public ApiErrorException(String code, String message, HttpStatus status) {
        this(code, message, status, null);
    }

    public ApiErrorException(String code, String message, HttpStatus status, Map<String, String> fieldErrors) {
        super(Objects.requireNonNullElse(message, code));
        this.code = Objects.requireNonNull(code, "code");
        this.status = Objects.requireNonNull(status, "status");
        this.fieldErrors = fieldErrors;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}


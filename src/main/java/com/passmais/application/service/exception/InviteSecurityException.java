package com.passmais.application.service.exception;

import org.springframework.http.HttpStatus;

public class InviteSecurityException extends RuntimeException {
    private final HttpStatus status;

    public InviteSecurityException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

package com.app.prod.exceptions;

import org.springframework.http.HttpStatus;

public record ErrorResponse(
        int status,
        String error,
        String message
) {
    public ErrorResponse(HttpStatus status, String message) {
        this(
                status.value(),
                status.getReasonPhrase(),
                message
        );
    }
}

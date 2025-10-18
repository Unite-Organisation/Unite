package com.app.prod.exceptions.exceptions;

public class UnauthorizedDataAccessException extends RuntimeException {
    public UnauthorizedDataAccessException(String message) {
        super(message);
    }
}

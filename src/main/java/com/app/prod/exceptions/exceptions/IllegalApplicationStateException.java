package com.app.prod.exceptions.exceptions;

public class IllegalApplicationStateException extends RuntimeException {
    public IllegalApplicationStateException(String message) {
        super(message);
    }
}

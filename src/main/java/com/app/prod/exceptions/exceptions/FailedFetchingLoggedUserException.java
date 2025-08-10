package com.app.prod.exceptions.exceptions;

public class FailedFetchingLoggedUserException extends RuntimeException {
    public FailedFetchingLoggedUserException(String message) {
        super(message);
    }
}

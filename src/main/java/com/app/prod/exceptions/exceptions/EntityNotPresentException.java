package com.app.prod.exceptions.exceptions;

public class EntityNotPresentException extends RuntimeException {
    public EntityNotPresentException(String message) {
        super(message);
    }
}

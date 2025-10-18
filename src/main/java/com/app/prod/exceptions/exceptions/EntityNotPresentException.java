package com.app.prod.exceptions.exceptions;

public class EntityNotPresentException extends RuntimeException {
    public EntityNotPresentException(String message, String entity) {
        super(String.format("Entity %s is not present. Error message: %s", entity, message));
    }
}

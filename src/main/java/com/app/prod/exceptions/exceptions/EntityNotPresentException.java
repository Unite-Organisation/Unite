package com.app.prod.exceptions.exceptions;

public class EntityNotPresentException extends RuntimeException {
    public EntityNotPresentException(String message) {
        super("[ENTITY WAS NOT PRESENT]: " + message);
    }
}

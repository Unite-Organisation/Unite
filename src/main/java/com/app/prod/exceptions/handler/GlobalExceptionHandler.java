package com.app.prod.exceptions.handler;

import com.app.prod.config.security.TokenSecurityManager;
import com.app.prod.exceptions.exceptions.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final TokenSecurityManager tokenSecurityManager;

    public GlobalExceptionHandler(TokenSecurityManager tokenSecurityManager) {
        this.tokenSecurityManager = tokenSecurityManager;
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<String> handleException(BadRequestException e){
        log.error("Wrong api request. Exception content: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleException(AccessDeniedException e){
        log.error("Access denied: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                e.getMessage() + "User: " + tokenSecurityManager.getCurrentUser().getUsername() +
                        "has role: " + tokenSecurityManager.getCurrentUser().getUserRole()
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e){
        log.error("Unhandled exception, returning status code 500. Exception content: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }

}

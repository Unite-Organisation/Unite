package com.app.prod.exceptions.handler;

import com.app.prod.config.security.TokenSecurityManager;
import com.app.prod.exceptions.ErrorResponse;
import com.app.prod.exceptions.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
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

    @ExceptionHandler(DataAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleException(DataAlreadyExistsException e){
        log.info("Data already exists in database. Exception content: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.CONFLICT, e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleException(AuthenticationException e){
        log.info("Wrong credentials. Exception content: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleException(BadRequestException e){
        log.error("Wrong api request. Exception content: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleException(AccessDeniedException e){
        log.error("Access denied: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                e.getMessage() + "User: " + tokenSecurityManager.getCurrentUser().getUsername() +
                        "has role: " + tokenSecurityManager.getCurrentUser().getUserRole()
                );
    }

    @ExceptionHandler(EntityNotPresentException.class)
    public ResponseEntity<String> handleException(EntityNotPresentException e){
        log.error("Entity was not present {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(UnauthorizedDataAccessException.class)
    public ResponseEntity<String> handleException(UnauthorizedDataAccessException e){
        log.error("Unauthorized access to resource. {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler(EmptyFileException.class)
    public ResponseEntity<String> handleException(EmptyFileException e){
        log.error("Bad request - file empty. Exception content: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<String> handleException(FileNotFoundException e){
        log.error("File with path saved in database does not exist. Exception content: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }

    @ExceptionHandler(InvalidFileExtensionException.class)
    public ResponseEntity<String> handleException(InvalidFileExtensionException e){
        log.error("Invalid file extension. Exception content: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        log.error("Unhandled exception, returning status code 500. Exception content: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }

}

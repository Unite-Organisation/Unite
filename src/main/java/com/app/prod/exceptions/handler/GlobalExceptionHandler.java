package com.app.prod.exceptions.handler;

import com.app.prod.exceptions.ApiErrorResponse;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.*;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // -------------------------------------------------------
    // AppExceptions

    @ExceptionHandler(DataAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleException(DataAlreadyExistsException e, HttpServletRequest request){
        log.info("Data already exists in database. Exception content: {}", e.getMessage());
        ApiErrorResponse errorResponse = ApiErrorResponse.of(HttpStatus.CONFLICT, e.getAppErrors(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleException(BadRequestException e, HttpServletRequest request){
        log.error("Wrong api request. Exception content: {}", e.getMessage());
        ApiErrorResponse errorResponse = ApiErrorResponse.of(HttpStatus.BAD_REQUEST, e.getAppErrors(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(EntityNotPresentException.class)
    public ResponseEntity<ApiErrorResponse> handleException(EntityNotPresentException e, HttpServletRequest request){
        log.error("Entity was not present {}", e.getMessage());
        ApiErrorResponse errorResponse = ApiErrorResponse.of(HttpStatus.NOT_FOUND, e.getAppErrors(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(UnauthorizedDataAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleException(UnauthorizedDataAccessException e, HttpServletRequest request){
        log.error("Unauthorized access to resource. {}", e.getMessage());
        ApiErrorResponse errorResponse = ApiErrorResponse.of(HttpStatus.FORBIDDEN, e.getAppErrors(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(EmptyFileException.class)
    public ResponseEntity<ApiErrorResponse> handleException(EmptyFileException e, HttpServletRequest request){
        log.error("Bad request - file empty. Exception content: {}", e.getMessage());
        ApiErrorResponse errorResponse = ApiErrorResponse.of(HttpStatus.BAD_REQUEST, e.getAppErrors(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleException(FileNotFoundException e, HttpServletRequest request){
        log.error("File with path saved in database does not exist. Exception content: {}", e.getMessage());
        ApiErrorResponse errorResponse = ApiErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, e.getAppErrors(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(InvalidFileExtensionException.class)
    public ResponseEntity<ApiErrorResponse> handleException(InvalidFileExtensionException e, HttpServletRequest request){
        log.error("Invalid file extension. Exception content: {}", e.getMessage());
        ApiErrorResponse errorResponse = ApiErrorResponse.of(HttpStatus.BAD_REQUEST, e.getAppErrors(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(IllegalApplicationStateException.class)
    public ResponseEntity<ApiErrorResponse> handleException(IllegalApplicationStateException e, HttpServletRequest request) {
        log.error("Invalid application state. Exception content: {}", e.getMessage());
        ApiErrorResponse errorResponse = ApiErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, e.getAppErrors(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    // -------------------------------------------------------
    // Spring / Java - Exceptions

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiErrorResponse> handleException(ExpiredJwtException e, HttpServletRequest request){
        ApiErrorResponse errorResponse = ApiErrorResponse.of(HttpStatus.UNAUTHORIZED, AppError.of(Code.JWT_TOKEN_EXPIRED), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleException(AccessDeniedException e, HttpServletRequest request){
        log.error("Access denied: {}", e.getMessage());
        ApiErrorResponse errorResponse = ApiErrorResponse.of(HttpStatus.FORBIDDEN, AppError.of(Code.ACCESS_DENIED), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleException(AuthenticationException e, HttpServletRequest request){
        log.info("Wrong credentials. Exception content: {}", e.getMessage());
        ApiErrorResponse errorResponse = ApiErrorResponse.of(HttpStatus.UNAUTHORIZED, AppError.of(Code.BAD_CREDENTIALS), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleException(MethodArgumentNotValidException e, HttpServletRequest request){
        List<AppError> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> AppError.of(Code.VALIDATION_ERROR, "%s %s".formatted(error.getField(), error.getDefaultMessage())))
                .toList();

        log.info("Request body failed validation: {}", errors);
        ApiErrorResponse errorResponse = ApiErrorResponse.of(HttpStatus.BAD_REQUEST, errors, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception, returning status code 500. Exception content: {}", e.getMessage());
        ApiErrorResponse errorResponse = ApiErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, AppError.of(Code.UNKNOWN_ERROR), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

}

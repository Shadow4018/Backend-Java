package com.java.tms.glossary.exception;

import com.java.tms.common.dto.ApplicationErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Error mapping of the glossary service. The response body type, {@link ApplicationErrorResponse},
 * is the one object shared with the other services through the "common" module.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApplicationErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());
        return new ResponseEntity<>(new ApplicationErrorResponse(ex, HttpStatus.BAD_REQUEST.value()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApplicationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        return new ResponseEntity<>(new ApplicationErrorResponse(ex, HttpStatus.BAD_REQUEST.value()), HttpStatus.BAD_REQUEST);
    }

    /** The "term" column is unique: adding the same term twice is a conflict, not a server error. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApplicationErrorResponse> handleDuplicate(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return new ResponseEntity<>(new ApplicationErrorResponse(ex, HttpStatus.CONFLICT.value()), HttpStatus.CONFLICT);
    }
}

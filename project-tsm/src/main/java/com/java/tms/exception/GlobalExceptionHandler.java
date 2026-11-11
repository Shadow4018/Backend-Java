package com.java.tms.exception;

import com.java.tms.async.TaskOverloadedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j // lombok annotation to generate a logger for this class
@RestControllerAdvice // not just controller, but a global exception handler for all controllers
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

    // AccessDeniedException from @PreAuthorize (AC7) is otherwise handled by Spring Security's
    // own entry point, but this makes the 403 body consistent with the rest of the API.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApplicationErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return new ResponseEntity<>(new ApplicationErrorResponse(ex, HttpStatus.FORBIDDEN.value()), HttpStatus.FORBIDDEN);
    }

    // Lab 5: a saturated executor is a deliberate, explicit "try again later" (503), never
    // an accidental, unmapped 500.
    @ExceptionHandler(TaskOverloadedException.class)
    public ResponseEntity<ApplicationErrorResponse> handleTaskOverloaded(TaskOverloadedException ex) {
        log.warn("Task rejected by a saturated executor: {}", ex.getMessage());
        return new ResponseEntity<>(new ApplicationErrorResponse(ex, HttpStatus.SERVICE_UNAVAILABLE.value()), HttpStatus.SERVICE_UNAVAILABLE);
    }
}

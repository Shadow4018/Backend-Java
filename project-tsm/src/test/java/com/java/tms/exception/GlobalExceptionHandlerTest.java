package com.java.tms.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Pure unit tests for {@link GlobalExceptionHandler} — Lab 2 (Unit Testing).
 * No mocks are even needed for most cases (the class has no collaborators of its own);
 * {@link MethodArgumentNotValidException} requires a mocked constructor argument only
 * because its own constructor demands one, not because we're testing a dependency of
 * GlobalExceptionHandler.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleIllegalArgument_anyMessage_returns400WithMessagePreserved() {
        // Arrange
        IllegalArgumentException ex = new IllegalArgumentException("Project not found: 42");

        // Act
        ResponseEntity<ApplicationErrorResponse> response = handler.handleIllegalArgument(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Project not found: 42");
        assertThat(response.getBody().getError()).isEqualTo("IllegalArgumentException");
        assertThat(response.getBody().getStatus()).isEqualTo(400);
    }

    @Test
    void handleAccessDenied_anyMessage_returns403NotedAsForbiddenInBody() {
        // Arrange
        AccessDeniedException ex = new AccessDeniedException("Only the author may edit it");

        // Act
        ResponseEntity<ApplicationErrorResponse> response = handler.handleAccessDenied(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getError()).isEqualTo("AccessDeniedException");
    }

    @Test
    void handleValidation_fieldErrorOnRequest_returns400() {
        // Arrange: MethodArgumentNotValidException requires a BindingResult-bearing
        // constructor argument it never asks us to inspect further here, so it's mocked
        // purely to satisfy the constructor, not as a collaborator under test.
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        org.mockito.Mockito.when(ex.getMessage()).thenReturn("name: must not be blank");

        // Act
        ResponseEntity<ApplicationErrorResponse> response = handler.handleValidation(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("name: must not be blank");
    }
}

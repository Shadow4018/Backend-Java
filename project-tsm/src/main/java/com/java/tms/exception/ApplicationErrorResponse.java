package com.java.tms.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ApplicationErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;

    public ApplicationErrorResponse(Exception ex, int status) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = ex.getClass().getSimpleName();
        this.message = ex.getMessage();
    }
}

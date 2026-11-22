package com.java.tms.async;

/**
 * Thrown deliberately (not a leaked framework exception) when a request can't be submitted
 * to a saturated executor. {@link com.java.tms.exception.GlobalExceptionHandler} maps this
 * to {@code 503 Service Unavailable} with a clear "try again later" message, rather than
 * letting the underlying {@code TaskRejectedException} surface as a confusing, unmapped
 * {@code 500}.
 */
public class TaskOverloadedException extends RuntimeException {

    public TaskOverloadedException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.java.tms.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Catches exceptions thrown by a {@code void}-returning {@code @Async} method.
 *
 * <p>A {@code CompletableFuture<T>}-returning {@code @Async} method reports its failure
 * through the future itself (see {@link ProjectExportService}'s use of
 * {@code exceptionally}/{@code handle}). A {@code void}-returning one has no such channel —
 * there is no future for the caller to inspect — so without this handler the exception would
 * simply vanish into the executor thread, unnoticed by anyone. Registering this handler
 * (via {@link AsyncConfig#getAsyncUncaughtExceptionHandler()}) is what makes that failure
 * visible instead of silent.
 */
public class LoggingAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {

    private static final Logger ASYNC_ERROR_LOG = LoggerFactory.getLogger("ASYNC-ERRORS");

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        ASYNC_ERROR_LOG.error("Uncaught exception in @Async void method {}#{} with args {}: {}",
                method.getDeclaringClass().getSimpleName(), method.getName(), Arrays.toString(params), ex.getMessage(), ex);
    }
}

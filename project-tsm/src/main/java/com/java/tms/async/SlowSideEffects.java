package com.java.tms.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The three slow domain side effects Lab 5 asks for — each one used to block whoever called
 * it for about a second. All three now run on {@link AsyncConfig#TMS_TASK_EXECUTOR}, our own
 * bounded pool, never on Spring's unbounded default executor.
 */
@Component
@Slf4j
public class SlowSideEffects {

    private static final long SIMULATED_DELAY_MS = 1000;

    private final ThreadPoolTaskExecutor narrowTaskExecutor;

    public SlowSideEffects(@Qualifier(AsyncConfig.NARROW_TASK_EXECUTOR) ThreadPoolTaskExecutor narrowTaskExecutor) {
        this.narrowTaskExecutor = narrowTaskExecutor;
    }

    /**
     * Side effect #1: "send an email". Fire-and-forget — {@code void} return type, nobody
     * ever gets (or needs) a {@link CompletableFuture} back. {@code ProjectService.create}
     * calls this and moves on immediately; the email is simulated here with a sleep so
     * there's something genuinely slow to not block.
     *
     * <p>Throws on a blank recipient purely so {@link LoggingAsyncUncaughtExceptionHandler}
     * has something real to catch in tests — a {@code void @Async} method's only way of
     * reporting failure.
     */
    @Async(AsyncConfig.TMS_TASK_EXECUTOR)
    public void sendProjectCreatedEmailAsync(String recipientEmail, String projectName) {
        simulateSlowWork();
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException("Cannot send project-created email: no recipient address");
        }
        log.info("[simulated email] To: {} - Project '{}' was created on thread {}",
                recipientEmail, projectName, Thread.currentThread().getName());
    }

    /**
     * Side effect #2: "generate a PDF report". Returns a {@link CompletableFuture} so the
     * caller ({@link ProjectExportService}) can run it alongside
     * {@link #fetchExternalGlossarySuggestionsAsync} and combine both results.
     */
    @Async(AsyncConfig.TMS_TASK_EXECUTOR)
    public CompletableFuture<String> generatePdfReportAsync(Integer projectId) {
        simulateSlowWork();
        String threadName = Thread.currentThread().getName();
        return CompletableFuture.completedFuture(
                "PDF report for project " + projectId + " (generated on thread " + threadName + ")");
    }

    /**
     * Side effect #3: "call an external service" (a machine-translation glossary-suggestion
     * API, say). Deliberately throws when {@code projectId} is {@code null}, simulating that
     * external call failing — the one input {@link ProjectExportService}'s tests use to
     * exercise the {@code exceptionally}/{@code handle} fallback path (AC: a failed async
     * call must not crash the caller).
     */
    @Async(AsyncConfig.TMS_TASK_EXECUTOR)
    public CompletableFuture<List<String>> fetchExternalGlossarySuggestionsAsync(Integer projectId) {
        simulateSlowWork();
        if (projectId == null) {
            // CompletableFuture.failedFuture, not a thrown exception: inside an @Async method
            // that returns a CompletableFuture, Spring already wraps an escaping exception
            // into a failed future for us, but constructing it explicitly here keeps the
            // "this call can fail" path visible and intentional rather than accidental.
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Simulated external glossary-suggestion service failure"));
        }
        String threadName = Thread.currentThread().getName();
        return CompletableFuture.completedFuture(
                List.of("suggestion-1 (thread " + threadName + ")", "suggestion-2"));
    }

    /**
     * Demonstrates the executor-overflow scenario on purpose: submits {@code task} to the
     * deliberately tiny {@link AsyncConfig#NARROW_TASK_EXECUTOR} (capacity for 2 in-flight
     * tasks total) and converts Spring's {@link TaskRejectedException} into our own
     * {@link TaskOverloadedException} — an explicit "try again later" decision, never a
     * silently swallowed failure or an accidental generic 500.
     */
    public void runOnNarrowExecutor(Runnable task) {
        try {
            narrowTaskExecutor.execute(task);
        } catch (TaskRejectedException ex) {
            throw new TaskOverloadedException(
                    "The task queue is full right now - please try again in a moment", ex);
        }
    }

    private void simulateSlowWork() {
        try {
            Thread.sleep(SIMULATED_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while simulating slow work", e);
        }
    }
}

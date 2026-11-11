package com.java.tms.async;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-Spring-context tests for {@link SlowSideEffects} — only a genuine {@code @Async}
 * proxy backed by our own {@link AsyncConfig#TMS_TASK_EXECUTOR} can show that a call returns
 * control immediately (Scenario "Повільний метод більше не блокує") and that it actually ran
 * on a thread from our named pool (Scenario "Виконавець є явно визначеним"), so a pure
 * Mockito test cannot cover these two - a real bean, obtained through
 * {@code @Autowired}, is required.
 */
@SpringBootTest
@ActiveProfiles("test")
class SlowSideEffectsAsyncTest {

    @Autowired
    private SlowSideEffects slowSideEffects;

    private ListAppender<ILoggingEvent> sideEffectsAppender;
    private ListAppender<ILoggingEvent> asyncErrorsAppender;

    @BeforeEach
    void attachAppenders() {
        sideEffectsAppender = attach(SlowSideEffects.class.getName());
        asyncErrorsAppender = attach("ASYNC-ERRORS");
    }

    @AfterEach
    void detachAppenders() {
        detach(SlowSideEffects.class.getName(), sideEffectsAppender);
        detach("ASYNC-ERRORS", asyncErrorsAppender);
    }

    @Test
    void fireAndForgetEmailReturnsImmediatelyAndCompletesLaterOnTheAsyncExecutor() throws InterruptedException {
        long before = System.nanoTime();
        slowSideEffects.sendProjectCreatedEmailAsync("team@tms.example.com", "Demo Project");
        long callDurationMs = (System.nanoTime() - before) / 1_000_000;

        // THEN it returns control well before the ~1s simulated send is done...
        assertThat(callDurationMs).isLessThan(300);
        assertThat(sideEffectsAppender.list).isEmpty(); // nothing logged yet - still "in flight"

        // AND it completes later, on its own, on a thread from our executor.
        waitUntil(() -> !sideEffectsAppender.list.isEmpty(), 3000);
        String logThreadName = sideEffectsAppender.list.get(0).getThreadName();
        assertThat(logThreadName).startsWith("tms-async-");
    }

    @Test
    void asyncMethodRunsOnOurNamedExecutorNotTheSpringBootDefault() throws Exception {
        CompletableFuture<String> future = slowSideEffects.generatePdfReportAsync(1);

        String report = future.get(3, TimeUnit.SECONDS);

        assertThat(report).contains("thread tms-async-");
    }

    @Test
    void aFailedAsyncCallIsHandledWithExceptionallyInsteadOfCrashingTheCaller() throws Exception {
        CompletableFuture<List<String>> future = slowSideEffects.fetchExternalGlossarySuggestionsAsync(null);

        CompletableFuture<List<String>> withFallback = future.exceptionally(ex -> List.of("fallback-suggestion"));

        // Does NOT throw back to the caller, even though the underlying call failed.
        List<String> result = withFallback.get(3, TimeUnit.SECONDS);
        assertThat(result).containsExactly("fallback-suggestion");
    }

    @Test
    void anUncaughtExceptionFromAVoidAsyncMethodIsCaughtByTheConfiguredHandlerNotLost() throws InterruptedException {
        // A blank recipient makes sendProjectCreatedEmailAsync throw internally; since it's
        // void, that exception has no Future to travel back on - only
        // LoggingAsyncUncaughtExceptionHandler can observe it.
        slowSideEffects.sendProjectCreatedEmailAsync("", "Some Project");

        waitUntil(() -> !asyncErrorsAppender.list.isEmpty(), 3000);

        assertThat(asyncErrorsAppender.list).hasSize(1);
        assertThat(asyncErrorsAppender.list.get(0).getFormattedMessage())
                .contains("sendProjectCreatedEmailAsync");
    }

    private ListAppender<ILoggingEvent> attach(String loggerName) {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        ((Logger) LoggerFactory.getLogger(loggerName)).addAppender(appender);
        return appender;
    }

    private void detach(String loggerName, ListAppender<ILoggingEvent> appender) {
        ((Logger) LoggerFactory.getLogger(loggerName)).detachAppender(appender);
    }

    private void waitUntil(java.util.function.BooleanSupplier condition, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("Condition was not met within " + timeoutMs + " ms");
            }
            Thread.sleep(50);
        }
    }
}

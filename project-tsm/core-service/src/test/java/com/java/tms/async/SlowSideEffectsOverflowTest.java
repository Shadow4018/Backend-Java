package com.java.tms.async;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit test (no Spring context) for the executor-overflow scenario: a
 * deliberately tiny {@link ThreadPoolTaskExecutor} (1 worker + a 1-slot queue, matching
 * {@link AsyncConfig#narrowTaskExecutor()}) is built by hand here and handed straight to a
 * {@link SlowSideEffects} instance, so a 3rd concurrent submission has nowhere to go.
 */
class SlowSideEffectsOverflowTest {

    private final ThreadPoolTaskExecutor narrowExecutor = buildNarrowExecutor();
    private final SlowSideEffects slowSideEffects = new SlowSideEffects(narrowExecutor);

    @AfterEach
    void shutdownExecutor() {
        narrowExecutor.shutdown();
    }

    @Test
    void aThirdConcurrentSubmissionIsRejectedWithOurOwnExplicitException() throws InterruptedException {
        // 1st submission: starts running immediately on the executor's single worker thread.
        CountDownLatch releaseFirstTask = new CountDownLatch(1);
        slowSideEffects.runOnNarrowExecutor(blockUntilReleased(releaseFirstTask));

        // 2nd submission: the worker is busy, so this one fills the single queue slot.
        CountDownLatch releaseSecondTask = new CountDownLatch(1);
        slowSideEffects.runOnNarrowExecutor(blockUntilReleased(releaseSecondTask));

        // Give the first task a moment to actually start (so it genuinely occupies the
        // worker thread before we judge the pool "full").
        Thread.sleep(50);

        // 3rd submission: worker busy AND queue full -> nowhere to go -> rejected.
        assertThatThrownBy(() -> slowSideEffects.runOnNarrowExecutor(() -> { }))
                .isInstanceOf(TaskOverloadedException.class)
                .hasMessageContaining("try again");

        releaseFirstTask.countDown();
        releaseSecondTask.countDown();
    }

    private Runnable blockUntilReleased(CountDownLatch latch) {
        return () -> {
            try {
                latch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
    }

    private ThreadPoolTaskExecutor buildNarrowExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("test-narrow-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setDaemon(true);
        executor.initialize();
        return executor;
    }
}

package com.java.tms.async;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test (no Spring context needed - these are plain {@code @Bean} factory methods)
 * proving the executor is explicitly, deliberately sized, not left to Spring Boot's default
 * {@code SimpleAsyncTaskExecutor} (which has no pool limits at all).
 */
class AsyncConfigTest {

    private final AsyncConfig config = new AsyncConfig();

    @Test
    void tmsTaskExecutorHasExplicitlyBoundedPoolAndQueueSizes() {
        ThreadPoolTaskExecutor executor = config.tmsTaskExecutor();

        assertThat(executor.getCorePoolSize()).isEqualTo(4);
        assertThat(executor.getMaxPoolSize()).isEqualTo(8);
        assertThat(executor.getThreadPoolExecutor().getQueue().remainingCapacity()).isEqualTo(50);
        assertThat(executor.getThreadNamePrefix()).isEqualTo("tms-async-");
    }

    @Test
    void narrowTaskExecutorIsDeliberatelyTinyForTheOverflowDemo() {
        ThreadPoolTaskExecutor executor = config.narrowTaskExecutor();

        assertThat(executor.getCorePoolSize()).isEqualTo(1);
        assertThat(executor.getMaxPoolSize()).isEqualTo(1);
        assertThat(executor.getThreadPoolExecutor().getQueue().remainingCapacity()).isEqualTo(1);
        assertThat(executor.getThreadNamePrefix()).isEqualTo("tms-narrow-");
    }

    @Test
    void asyncExecutorUsedByAtAsyncMethodsIsTheNamedTmsTaskExecutor() {
        // getAsyncExecutor() is what @EnableAsync actually wires every bare @Async method to
        // when no executor name is given - confirming it is NOT Spring's unbounded default.
        assertThat(config.getAsyncExecutor()).isInstanceOf(ThreadPoolTaskExecutor.class);
        assertThat(((ThreadPoolTaskExecutor) config.getAsyncExecutor()).getThreadNamePrefix())
                .isEqualTo("tms-async-");
    }

    @Test
    void uncaughtExceptionHandlerIsOurOwnLoggingOne() {
        assertThat(config.getAsyncUncaughtExceptionHandler())
                .isInstanceOf(LoggingAsyncUncaughtExceptionHandler.class);
    }
}

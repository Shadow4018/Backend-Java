package com.java.tms.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Lab 5 (Async): a dedicated, explicitly-sized thread pool for {@code @Async} work, instead
 * of Spring's default {@code SimpleAsyncTaskExecutor} (which spins up an unbounded number of
 * brand-new threads — exactly the "resource exhaustion" failure mode this lab warns about).
 *
 * <p>Two executors are defined on purpose:
 * <ul>
 *   <li>{@code tmsTaskExecutor} — the real one, used by every {@code @Async} method in this
 *       app (see {@link SlowSideEffects}, {@link ProjectExportService}).</li>
 *   <li>{@code narrowTaskExecutor} — deliberately tiny (capacity for exactly 2 tasks at a
 *       time), used only to demonstrate and test the overflow-handling scenario in
 *       {@link SlowSideEffects#runOnNarrowExecutor(Runnable)} — it is never wired into real
 *       request-handling paths.</li>
 * </ul>
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    public static final String TMS_TASK_EXECUTOR = "tmsTaskExecutor";
    public static final String NARROW_TASK_EXECUTOR = "narrowTaskExecutor";

    @Bean(name = TMS_TASK_EXECUTOR)
    public ThreadPoolTaskExecutor tmsTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("tms-async-");
        // Explicit, not Spring's implicit default: once the pool AND the queue are both
        // full, reject immediately with an exception rather than silently blocking the
        // caller or growing threads without bound.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        // Daemon so a lingering ~1s simulated task never keeps a test run or the JVM alive
        // after everything else has finished.
        executor.setDaemon(true);
        executor.initialize();
        return executor;
    }

    /**
     * core=1, max=1, queue capacity=1 → at most 2 tasks can ever be "in flight" at once (one
     * running, one queued). A 3rd concurrent submission is rejected — used only by
     * {@link SlowSideEffects#runOnNarrowExecutor(Runnable)} to deliberately exercise and test
     * the "executor overflow" scenario (AC: "Переповнення виконавця обробляється
     * цілеспрямовано").
     */
    @Bean(name = NARROW_TASK_EXECUTOR)
    public ThreadPoolTaskExecutor narrowTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("tms-narrow-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setDaemon(true);
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return tmsTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new LoggingAsyncUncaughtExceptionHandler();
    }
}

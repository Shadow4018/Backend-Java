package com.java.tms.aop.aspect;

import com.java.tms.aop.annotation.ExecutionTimed;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * Advice for {@link ExecutionTimed}: wraps the annotated method call, measures how long it
 * took, and logs the result — WARN once the method's own configured threshold is crossed,
 * INFO otherwise. {@code @Around} is required here (rather than {@code @Before}/
 * {@code @After}) because only {@code @Around} advice can see both "before" and "after" in
 * the same invocation to compute elapsed time, and it must always call {@link
 * ProceedingJoinPoint#proceed()} exactly once so the real method still runs and its
 * result/exception is still returned to the caller untouched.
 */
@Aspect
@Component
@Slf4j
public class TimingAspect {

    @Around("@annotation(executionTimed)")
    public Object measure(ProceedingJoinPoint joinPoint, ExecutionTimed executionTimed) throws Throwable {
        long startNanos = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            String description = describe(joinPoint);
            if (elapsedMs >= executionTimed.slowThresholdMs()) {
                log.warn("{} took {} ms (>= {} ms threshold) - slow operation", description, elapsedMs, executionTimed.slowThresholdMs());
            } else {
                log.info("{} took {} ms", description, elapsedMs);
            }
        }
    }

    private String describe(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "#" + signature.getName();
    }
}

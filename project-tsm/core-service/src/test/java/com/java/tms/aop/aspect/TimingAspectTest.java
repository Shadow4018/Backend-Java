package com.java.tms.aop.aspect;

import com.java.tms.aop.annotation.ExecutionTimed;
import org.junit.jupiter.api.Test;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure Mockito unit test for {@link TimingAspect} — the lab's "advice tested directly"
 * scenario: {@link ProceedingJoinPoint} is mocked, no Spring context and no
 * {@code @SpringBootTest} anywhere here. Checks the advice's actual decision logic: it
 * always calls {@code proceed()} exactly once and returns/propagates exactly what
 * {@code proceed()} produced, regardless of timing.
 */
class TimingAspectTest {

    private final TimingAspect aspect = new TimingAspect();

    @Test
    void callsProceedExactlyOnceAndReturnsItsResult() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint("SomeService", "someMethod");
        when(joinPoint.proceed()).thenReturn("the-result");
        ExecutionTimed annotation = annotationWithThreshold(200);

        Object result = aspect.measure(joinPoint, annotation);

        assertThat(result).isEqualTo("the-result");
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void propagatesExceptionsFromProceedWithoutSwallowingThem() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint("SomeService", "someMethod");
        IllegalStateException boom = new IllegalStateException("boom");
        when(joinPoint.proceed()).thenThrow(boom);
        ExecutionTimed annotation = annotationWithThreshold(200);

        assertThatThrownBy(() -> aspect.measure(joinPoint, annotation))
                .isSameAs(boom);
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void stillCallsProceedWhenThresholdIsZero() throws Throwable {
        // With slowThresholdMs = 0, every call falls into the WARN branch — this is exactly
        // what SelfInvocationDemoService#timedOperation uses so the aspect's effect (or lack
        // of it) is unmistakable in the logs; here we just confirm proceed() still runs.
        ProceedingJoinPoint joinPoint = mockJoinPoint("SelfInvocationDemoService", "timedOperation");
        when(joinPoint.proceed()).thenReturn("timed-operation-result");
        ExecutionTimed annotation = annotationWithThreshold(0);

        Object result = aspect.measure(joinPoint, annotation);

        assertThat(result).isEqualTo("timed-operation-result");
        verify(joinPoint, times(1)).proceed();
    }

    private ProceedingJoinPoint mockJoinPoint(String declaringTypeSimpleName, String methodName) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn(methodName);
        // getDeclaringType() must return a real Class<?>; which one is irrelevant to the
        // test, only its simple name (used for the log message) is observed indirectly.
        doReturnClass(signature, declaringTypeSimpleName);
        return joinPoint;
    }

    private void doReturnClass(MethodSignature signature, String declaringTypeSimpleName) {
        Class<?> type = "SelfInvocationDemoService".equals(declaringTypeSimpleName)
                ? com.java.tms.aop.selfinvocation.SelfInvocationDemoService.class
                : TimingAspectTest.class;
        when(signature.getDeclaringType()).thenReturn(type);
    }

    private ExecutionTimed annotationWithThreshold(long thresholdMs) {
        ExecutionTimed annotation = mock(ExecutionTimed.class);
        when(annotation.slowThresholdMs()).thenReturn(thresholdMs);
        return annotation;
    }
}

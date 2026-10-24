package com.java.tms.aop.aspect;

import com.java.tms.aop.annotation.Cached;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure Mockito unit test for {@link CachingAspect} — this is the aspect the lab's
 * "whether proceed() is called, or what happens if not" scenario fits best: a cache hit
 * must skip {@code proceed()} entirely, a miss must call it.
 */
class CachingAspectTest {

    private final CachingAspect aspect = new CachingAspect();

    @Test
    void firstCallIsACacheMissAndInvokesProceed() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint("listAll", new Object[0]);
        when(joinPoint.proceed()).thenReturn(List.of("a", "b"));
        Cached annotation = annotationWithTtl(60_000);

        Object result = aspect.cacheResult(joinPoint, annotation);

        assertThat(result).isEqualTo(List.of("a", "b"));
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void secondCallWithinTtlIsACacheHitAndNeverCallsProceedAgain() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint("listAll", new Object[0]);
        when(joinPoint.proceed()).thenReturn(List.of("a", "b"));
        Cached annotation = annotationWithTtl(60_000);

        Object first = aspect.cacheResult(joinPoint, annotation);  // miss -> warms the cache
        Object second = aspect.cacheResult(joinPoint, annotation); // hit -> must NOT call proceed() again

        assertThat(second).isEqualTo(first);
        verify(joinPoint, times(1)).proceed(); // proceed() called exactly once across both calls
    }

    @Test
    void differentArgumentsAreCachedUnderDifferentKeys() throws Throwable {
        ProceedingJoinPoint callForProjectOne = mockJoinPoint("getById", new Object[]{1});
        when(callForProjectOne.proceed()).thenReturn("project-1");
        ProceedingJoinPoint callForProjectTwo = mockJoinPoint("getById", new Object[]{2});
        when(callForProjectTwo.proceed()).thenReturn("project-2");
        Cached annotation = annotationWithTtl(60_000);

        Object resultOne = aspect.cacheResult(callForProjectOne, annotation);
        Object resultTwo = aspect.cacheResult(callForProjectTwo, annotation);

        assertThat(resultOne).isEqualTo("project-1");
        assertThat(resultTwo).isEqualTo("project-2");
        verify(callForProjectOne, times(1)).proceed();
        verify(callForProjectTwo, times(1)).proceed();
    }

    @Test
    void anExpiredEntryCausesASecondCallToProceedAgain() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint("listAll", new Object[0]);
        when(joinPoint.proceed()).thenReturn(List.of("a"));
        Cached annotation = annotationWithTtl(0); // expires almost immediately

        aspect.cacheResult(joinPoint, annotation);
        Thread.sleep(5); // make sure the TTL has genuinely elapsed before the second call
        aspect.cacheResult(joinPoint, annotation);

        verify(joinPoint, times(2)).proceed();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ProceedingJoinPoint mockJoinPoint(String methodName, Object[] args) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn(methodName);
        when(signature.getDeclaringType()).thenReturn((Class) CachingAspectTest.class);
        when(joinPoint.getArgs()).thenReturn(args);
        return joinPoint;
    }

    private Cached annotationWithTtl(long ttlMs) {
        Cached annotation = mock(Cached.class);
        when(annotation.ttlMs()).thenReturn(ttlMs);
        return annotation;
    }
}

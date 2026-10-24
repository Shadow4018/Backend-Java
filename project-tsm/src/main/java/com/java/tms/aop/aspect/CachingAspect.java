package com.java.tms.aop.aspect;

import com.java.tms.aop.annotation.Cached;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advice for {@link Cached}: a minimal in-memory, per-argument, TTL-based cache.
 *
 * <p>This is the one aspect among the three that can genuinely change what happens, not
 * just observe it: on a cache hit it returns the stored value and deliberately never calls
 * {@link ProceedingJoinPoint#proceed()}, so the annotated method's body does not run at
 * all for that call. On a miss, it calls {@code proceed()} exactly once and remembers the
 * result for next time.
 */
@Aspect
@Component
@Slf4j
public class CachingAspect {

    private record CacheKey(String method, List<Object> args) {
    }

    private record CacheEntry(Object value, long expiresAtNanos) {
        boolean isExpired(long nowNanos) {
            return nowNanos >= expiresAtNanos;
        }
    }

    private final Map<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();

    @Around("@annotation(cached)")
    public Object cacheResult(ProceedingJoinPoint joinPoint, Cached cached) throws Throwable {
        CacheKey key = keyFor(joinPoint);
        long now = System.nanoTime();
        CacheEntry existing = cache.get(key);

        if (existing != null && !existing.isExpired(now)) {
            log.info("Cache hit for {} - skipping method body", key.method());
            return existing.value();
        }

        log.info("Cache miss for {} - invoking method body", key.method());
        Object result = joinPoint.proceed();
        cache.put(key, new CacheEntry(result, now + cached.ttlMs() * 1_000_000L));
        return result;
    }

    private CacheKey keyFor(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringType().getSimpleName() + "#" + signature.getName();
        return new CacheKey(methodName, Arrays.asList(joinPoint.getArgs()));
    }
}

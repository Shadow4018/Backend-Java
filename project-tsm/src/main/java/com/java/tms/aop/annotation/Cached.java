package com.java.tms.aop.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a read-only method whose result should be cached in memory, keyed by its
 * arguments, for a short TTL, by {@link com.java.tms.aop.aspect.CachingAspect}.
 *
 * <p>Unlike {@link ExecutionTimed} or {@link AuditChange}, which only ever observe a call,
 * this concern can change what actually happens: on a cache hit the aspect returns the
 * stored value and deliberately never calls {@code proceed()} at all, so the annotated
 * method's body does not run.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cached {

    /** How long a cached result stays valid, in milliseconds. */
    long ttlMs() default 30_000;
}

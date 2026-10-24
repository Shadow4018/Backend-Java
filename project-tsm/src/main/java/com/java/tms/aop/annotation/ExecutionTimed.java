package com.java.tms.aop.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method whose wall-clock execution time should be measured and logged by
 * {@link com.java.tms.aop.aspect.TimingAspect} — a performance-monitoring cross-cutting
 * concern, not a renamed copy of a plain method-entry/exit logging aspect: it measures
 * elapsed time and escalates to a WARN log once a configurable "slow operation" threshold
 * is crossed.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExecutionTimed {

    /** Elapsed time (ms) at or above which the call is logged as a slow-operation WARN. */
    long slowThresholdMs() default 200;
}

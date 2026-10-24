package com.java.tms.aop.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a mutating service method whose successful completion should be recorded in the
 * audit trail (who did it, with which arguments, with what result) by
 * {@link com.java.tms.aop.aspect.AuditAspect}.
 *
 * <p>This is a different cross-cutting concern from plain logging: it fires only
 * on a normal return ({@code @AfterReturning}), so a failed/aborted mutation deliberately
 * leaves no audit entry — an audit trail records what actually happened, not what was
 * attempted.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditChange {

    /** Short, human-readable action name recorded in the audit entry, "CREATE_PROJECT" as exampl. */
    String action();
}

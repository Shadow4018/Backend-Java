package com.java.tms.aop.aspect;

import com.java.tms.aop.annotation.AuditChange;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Advice for {@link AuditChange}: after a mutating service method returns normally, records
 * who performed it, with what arguments, and what it returned.
 *
 * <p>{@code @AfterReturning} is deliberately used instead of {@code @Around}: an audit
 * aspect should never be able to change the return value or swallow an exception, it should
 * only observe a call that genuinely succeeded — if the method throws, {@code audit(...)}
 * never runs and nothing is recorded, which is the correct behaviour for an audit trail.
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("AUDIT");

    @AfterReturning(pointcut = "@annotation(auditChange)", returning = "result")
    public void audit(JoinPoint joinPoint, AuditChange auditChange, Object result) {
        AUDIT_LOG.info("action={} actor={} args={} result={}",
                auditChange.action(), currentUsername(), Arrays.toString(joinPoint.getArgs()), result);
    }

    // Package-private (not private) so it can be unit-tested directly, without a Spring
    // context, as its own piece of decision logic.
    String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "anonymous";
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            if (preferredUsername != null) {
                return preferredUsername;
            }
        }
        return authentication.getName();
    }
}

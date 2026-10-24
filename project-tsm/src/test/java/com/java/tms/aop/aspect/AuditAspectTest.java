package com.java.tms.aop.aspect;

import com.java.tms.aop.annotation.AuditChange;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure Mockito/no-Spring-context unit test for {@link AuditAspect}. {@code audit(...)} only
 * writes to a logger, so the real "decision logic" worth testing in isolation is
 * {@link AuditAspect#currentUsername()} — exactly the kind of branch-by-branch behaviour the
 * lab asks to verify directly rather than through a full {@code @SpringBootTest}.
 */
class AuditAspectTest {

    private final AuditAspect aspect = new AuditAspect();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readsPreferredUsernameClaimFromJwtPrincipal() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("preferred_username", "ivan.translator")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));

        assertThat(aspect.currentUsername()).isEqualTo("ivan.translator");
    }

    @Test
    void fallsBackToAuthenticationNameWhenJwtHasNoPreferredUsernameClaim() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "fallback-user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(jwt, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(aspect.currentUsername()).isEqualTo(authentication.getName());
    }

    @Test
    void fallsBackToAuthenticationNameForNonJwtPrincipals() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("belialu", "n/a"));

        assertThat(aspect.currentUsername()).isEqualTo("belialu");
    }

    @Test
    void reportsAnonymousWhenThereIsNoAuthenticationAtAll() {
        SecurityContextHolder.clearContext();

        assertThat(aspect.currentUsername()).isEqualTo("anonymous");
    }

    @Test
    void auditDoesNotThrowForANormalReturnValue() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("belialu", "n/a"));
        JoinPoint joinPoint = mock(JoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{42});
        AuditChange annotation = mock(AuditChange.class);
        when(annotation.action()).thenReturn("DELETE_PROJECT");

        assertThatCode(() -> aspect.audit(joinPoint, annotation, null)).doesNotThrowAnyException();
    }
}

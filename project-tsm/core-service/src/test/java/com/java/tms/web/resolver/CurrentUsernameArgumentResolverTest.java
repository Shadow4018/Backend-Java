package com.java.tms.web.resolver;

import com.java.tms.web.annotation.CurrentUsername;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.NativeWebRequest;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure Mockito unit test (no Spring context) for {@link CurrentUsernameArgumentResolver},
 * following the same isolation style as the Lab 2 service tests.
 */
class CurrentUsernameArgumentResolverTest {

    private final CurrentUsernameArgumentResolver resolver = new CurrentUsernameArgumentResolver();

    // --- supportsParameter (AC6) ---------------------------------------------------------

    @Test
    void supportsParameterAnnotatedWithCurrentUsername() throws NoSuchMethodException {
        MethodParameter annotated = parameterOf("annotatedMethod", 0);

        assertThat(resolver.supportsParameter(annotated)).isTrue();
    }

    @Test
    void doesNotSupportParameterWithoutTheAnnotation() throws NoSuchMethodException {
        MethodParameter plain = parameterOf("plainMethod", 0);

        assertThat(resolver.supportsParameter(plain)).isFalse();
    }

    // --- resolveArgument (AC7) ------------------------------------------------------------

    @Test
    void resolvesPreferredUsernameClaimFromJwtPrincipal() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("preferred_username", "ivan.translator")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(jwt, null);

        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        when(webRequest.getUserPrincipal()).thenReturn(authentication);

        Object resolved = resolver.resolveArgument(null, null, webRequest, null);

        assertThat(resolved).isEqualTo("ivan.translator");
    }

    @Test
    void fallsBackToAuthenticationNameWhenThereIsNoPreferredUsernameClaim() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "fallback-user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(jwt, null);

        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        when(webRequest.getUserPrincipal()).thenReturn(authentication);

        Object resolved = resolver.resolveArgument(null, null, webRequest, null);

        assertThat(resolved).isEqualTo(authentication.getName());
    }

    @Test
    void returnsNullWhenThereIsNoAuthenticatedPrincipal() {
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        when(webRequest.getUserPrincipal()).thenReturn(null);

        Object resolved = resolver.resolveArgument(null, null, webRequest, null);

        assertThat(resolved).isNull();
    }

    // --- test fixtures ---------------------------------------------------------------------

    @SuppressWarnings("unused")
    private void annotatedMethod(@CurrentUsername String username) {
    }

    @SuppressWarnings("unused")
    private void plainMethod(String username) {
    }

    private MethodParameter parameterOf(String methodName, int index) throws NoSuchMethodException {
        Method method = CurrentUsernameArgumentResolverTest.class.getDeclaredMethod(methodName, String.class);
        return new MethodParameter(method, index);
    }
}

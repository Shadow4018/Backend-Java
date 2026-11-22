package com.java.tms.web.resolver;

import com.java.tms.web.annotation.CurrentUsername;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.security.Principal;

/**
 * Resolves {@code @CurrentUsername String} controller parameters by pulling the username
 * out of the request's authentication context (AC6/AC7), the same way
 * {@code TranslationController#usernameOf(Authentication)} used to do it by hand in three
 * separate methods.
 */
public class CurrentUsernameArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // AC6: true only for a parameter carrying @CurrentUsername, false for any other.
        return parameter.hasParameterAnnotation(CurrentUsername.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) {
        Principal principal = webRequest.getUserPrincipal();
        if (principal == null) {
            return null;
        }

        // Our JWT resource-server flow hands us an Authentication whose principal is the
        // Jwt itself; prefer the human-readable "preferred_username" claim Keycloak puts
        // there (AC7) over the JWT's "sub", mirroring the controller's previous logic.
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof Jwt jwt) {
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            if (preferredUsername != null) {
                return preferredUsername;
            }
        }

        return principal.getName();
    }
}

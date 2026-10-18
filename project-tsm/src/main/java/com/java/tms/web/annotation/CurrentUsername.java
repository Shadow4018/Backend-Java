package com.java.tms.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method parameter that should receive the username of the currently
 * authenticated caller, extracted from the request's JWT (its {@code preferred_username}
 * claim, falling back to the authentication's own name) by
 * {@link com.java.tms.web.resolver.CurrentUsernameArgumentResolver}.
 *
 * <p>Before this annotation existed, every controller method that needed "who is calling
 * this?" duplicated the same {@code Authentication -> username} extraction logic
 * ({@code usernameOf(authentication)} in {@code TranslationController}); this annotation
 * lets Spring MVC do that extraction once, centrally, and simply hand the method the
 * value it needs.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUsername {
}

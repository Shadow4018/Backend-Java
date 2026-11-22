package com.java.tms.web.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Composed annotation: every "create" endpoint in this codebase is a POST mapping that
 * must also answer {@code 201 Created} rather than Spring MVC's default {@code 200 OK}
 * for a non-{@code ResponseEntity} return value — that exact pair was previously repeated
 * verbatim in four different controllers.
 *
 * <p>This meta-annotates with {@code @RequestMapping(method = POST)} rather than with
 * {@code @PostMapping} itself, because that is literally how Spring builds
 * {@code @PostMapping}/{@code @GetMapping} internally: {@code @PostMapping}'s own
 * {@code @Target} is {@code METHOD} only, so it cannot legally meta-annotate another
 * annotation declaration, while {@code @RequestMapping}'s {@code @Target} includes
 * {@code TYPE} precisely so it can. {@code @PostMappingCreated} is itself annotated with
 * {@code @RequestMapping(method = RequestMethod.POST)} and
 * {@code @ResponseStatus(HttpStatus.CREATED)}, so {@code RequestMappingHandlerMapping} and
 * {@code ServletInvocableHandlerMethod} find those two annotations via Spring's
 * merged-annotation lookup ({@code AnnotatedElementUtils}) and behave exactly as if both
 * had been written on the method directly (AC4).
 *
 * <p>{@code value()} is aliased onto {@code @RequestMapping}'s own {@code value} (= {@code path})
 * attribute via {@code @AliasFor} — the same mechanism {@code @PostMapping} itself uses — so
 * the path can still be set normally: {@code @PostMappingCreated("/text-keys")}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(method = RequestMethod.POST)
@ResponseStatus(HttpStatus.CREATED)
public @interface PostMappingCreated {

    @AliasFor(annotation = RequestMapping.class, attribute = "value")
    String[] value() default {};
}

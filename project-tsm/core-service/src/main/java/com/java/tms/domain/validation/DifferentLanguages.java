package com.java.tms.domain.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level Bean Validation constraint: a project's (or translation task's) source
 * language and target language must not be the same language — "translating" a language
 * into itself is never a meaningful request in this domain.
 *
 * <p>This is a genuinely new business rule (comparing two fields of the annotated object
 * to each other), not a renamed copy of the lecture's {@code @ValidSkuCategory}, which
 * checks a single field against a fixed set of allowed values.
 *
 * <p>Any DTO that implements {@link LanguagePair} can be annotated with this constraint;
 * {@link DifferentLanguagesValidator} is picked up automatically by Bean Validation purely
 * from {@code @Constraint(validatedBy = ...)} below (AC1) — nothing in application code ever
 * creates a {@code DifferentLanguagesValidator} instance by hand.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = DifferentLanguagesValidator.class)
public @interface DifferentLanguages {

    String message() default "Source language and target language must be different";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

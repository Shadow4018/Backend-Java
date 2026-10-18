package com.java.tms.domain.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates {@link DifferentLanguages}. Bean Validation instantiates this class itself
 * (via the no-arg constructor the compiler generates) the moment it finds the
 * {@code @Constraint(validatedBy = DifferentLanguagesValidator.class)} reference on the
 * annotation — see AC1.
 */
public class DifferentLanguagesValidator implements ConstraintValidator<DifferentLanguages, LanguagePair> {

    @Override
    public boolean isValid(LanguagePair value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // absence of the whole object is someone else's problem (e.g. @NotNull)
        }

        String source = value.getSourceLanguage();
        String target = value.getTargetLanguage();
        if (source == null || target == null || source.isBlank() || target.isBlank()) {
            // Missing/blank values are reported by @NotBlank on the fields themselves;
            // this constraint only judges whether two *present* languages differ (AC2).
            return true;
        }

        boolean different = !source.trim().equalsIgnoreCase(target.trim());
        if (!different) {
            // AC3: a specific message naming the actual offending value, not a generic
            // "invalid" — replaces the annotation's default message with this one.
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            "Source language and target language must be different, but both are '"
                                    + source.trim() + "'")
                    .addConstraintViolation();
        }
        return different;
    }
}

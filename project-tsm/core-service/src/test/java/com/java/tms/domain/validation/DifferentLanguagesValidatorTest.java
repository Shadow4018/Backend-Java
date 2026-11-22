package com.java.tms.domain.validation;

import com.java.tms.domain.dto.ProjectCreateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure Bean Validation unit test (no Spring context) for the {@link DifferentLanguages}
 * constraint, in the same spirit as the Lab 2 Mockito-isolated service tests: it drives a
 * plain {@link Validator} directly and asserts on real violations.
 */
class DifferentLanguagesValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        factory.close();
    }

    @Test
    void differentLanguagesProduceNoViolation() {
        // AC1: we never instantiate DifferentLanguagesValidator ourselves — validator.validate(...)
        // is the only way this test (or Bean Validation itself) ever touches it.
        ProjectCreateRequest request = new ProjectCreateRequest("Demo", "desc", "en", "uk");

        Set<ConstraintViolation<ProjectCreateRequest>> violations = validator.validate(request);

        // AC2: valid, differing languages -> no violation of this constraint.
        assertThat(violations).noneMatch(v -> v.getConstraintDescriptor().getAnnotation() instanceof DifferentLanguages);
    }

    @Test
    void sameLanguagesProduceASpecificViolation() {
        ProjectCreateRequest request = new ProjectCreateRequest("Demo", "desc", "en", "en");

        Set<ConstraintViolation<ProjectCreateRequest>> violations = validator.validate(request);

        // AC3: a violation is raised, and its message names the actual offending value
        // rather than a generic "invalid" / "must not be blank" style message.
        assertThat(violations)
                .filteredOn(v -> v.getConstraintDescriptor().getAnnotation() instanceof DifferentLanguages)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .singleElement()
                .satisfies(message -> {
                    assertThat(message).contains("must be different");
                    assertThat(message).contains("'en'");
                });
    }

    @Test
    void comparisonIsCaseInsensitiveAndIgnoresSurroundingWhitespace() {
        ProjectCreateRequest request = new ProjectCreateRequest("Demo", "desc", " EN ", "en");

        Set<ConstraintViolation<ProjectCreateRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getConstraintDescriptor().getAnnotation() instanceof DifferentLanguages);
    }

    @Test
    void blankLanguagesAreLeftToNotBlankRatherThanFlaggedHere() {
        // sourceLanguage/targetLanguage are blank -> @NotBlank fires, but DifferentLanguages
        // itself must not also complain about two blank (and therefore "equal") strings.
        ProjectCreateRequest request = new ProjectCreateRequest("Demo", "desc", "", "");

        Set<ConstraintViolation<ProjectCreateRequest>> violations = validator.validate(request);

        assertThat(violations).noneMatch(v -> v.getConstraintDescriptor().getAnnotation() instanceof DifferentLanguages);
        assertThat(violations).isNotEmpty(); // @NotBlank still fires
    }
}

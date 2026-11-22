package com.java.tms.glossary.repository;

import com.java.tms.glossary.model.GlossaryTerm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The service's own database layer: the schema comes from the service's OWN Liquibase
 * changelog (not from JPA and not from core-service).
 */
@DataJpaTest
@ActiveProfiles("test")
class GlossaryTermRepositoryTest {

    @Autowired
    private GlossaryTermRepository repository;

    @Test
    void aSavedTermCanBeReadBack() {
        GlossaryTerm saved = repository.save(new GlossaryTerm(null, "Sinner", "Грішник", "main characters"));

        assertThat(repository.findById(saved.getId()))
                .get()
                .extracting(GlossaryTerm::getTranslation)
                .isEqualTo("Грішник");
    }

    @Test
    void theTermColumnIsUniqueAccordingToTheLiquibaseSchema() {
        repository.saveAndFlush(new GlossaryTerm(null, "Abnormality", "Аномалія", null));

        assertThatThrownBy(() -> repository.saveAndFlush(new GlossaryTerm(null, "Abnormality", "Інше", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

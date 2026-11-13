package com.java.tms.service;

import com.java.tms.domain.enums.TranslationStatus;
import com.java.tms.domain.model.Project;
import com.java.tms.domain.model.TextKey;
import com.java.tms.domain.model.Translation;
import com.java.tms.repository.ProjectRepository;
import com.java.tms.repository.TextKeyRepository;
import com.java.tms.repository.TranslationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lab 6 integration test ("it"): the job's logic is safe to run twice in a row - the second run
 * changes nothing - against a real (H2) database through the real repository.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TranslationCleanupService.class)
class TranslationCleanupServiceIntegrationTest {

    @Autowired
    private TranslationCleanupService cleanupService;
    @Autowired
    private TranslationRepository translationRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private TextKeyRepository textKeyRepository;

    private TextKey textKey;

    @BeforeEach
    void seedProjectAndTextKey() {
        Project project = new Project();
        project.setName("Cleanup IT project");
        project.setSourceLanguage("en");
        project.setTargetLanguage("uk");
        project = projectRepository.save(project);

        TextKey key = new TextKey();
        key.setKeyName("greeting");
        key.setSourceText("Hello");
        key.setProject(project);
        textKey = textKeyRepository.save(key);
    }

    @Test
    void runningTheCleanupTwiceInARowLeavesTheSecondRunWithNothingToDo() {
        addTranslation(TranslationStatus.REJECTED);
        addTranslation(TranslationStatus.REJECTED);
        addTranslation(TranslationStatus.REJECTED);
        addTranslation(TranslationStatus.APPROVED);
        addTranslation(TranslationStatus.PENDING);

        int firstRun = cleanupService.purgeRejected(100, "it");
        long afterFirst = translationRepository.count();
        int secondRun = cleanupService.purgeRejected(100, "it");
        long afterSecond = translationRepository.count();

        assertThat(firstRun).isEqualTo(3);
        assertThat(afterFirst).isEqualTo(2);          // APPROVED + PENDING survive
        assertThat(secondRun).isZero();               // idempotent: nothing left to delete
        assertThat(afterSecond).isEqualTo(afterFirst); // and the data did not change
        assertThat(translationRepository.findAll())
                .extracting(Translation::getStatus)
                .containsExactlyInAnyOrder(TranslationStatus.APPROVED, TranslationStatus.PENDING);
    }

    @Test
    void theBatchLimitIsRespectedAndRepeatedRunsConvergeToZero() {
        for (int i = 0; i < 5; i++) {
            addTranslation(TranslationStatus.REJECTED);
        }

        assertThat(cleanupService.purgeRejected(2, "it")).isEqualTo(2);
        assertThat(cleanupService.purgeRejected(2, "it")).isEqualTo(2);
        assertThat(cleanupService.purgeRejected(2, "it")).isEqualTo(1);
        assertThat(cleanupService.purgeRejected(2, "it")).isZero();
        assertThat(translationRepository.count()).isZero();
    }

    @Test
    void aNonPositiveLimitIsRejectedInsteadOfDeletingEverythingOrNothingByAccident() {
        addTranslation(TranslationStatus.REJECTED);

        assertThatThrownBy(() -> cleanupService.purgeRejected(0, "it"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(translationRepository.count()).isEqualTo(1);
    }

    private void addTranslation(TranslationStatus status) {
        Translation translation = new Translation();
        translation.setTextKey(textKey);
        translation.setLanguageCode("uk");
        translation.setTranslatedText("Привіт");
        translation.setTranslatorName("tester");
        translation.setStatus(status);
        translationRepository.save(translation);
    }
}

package com.java.tms.service;

import com.java.tms.domain.dto.ProjectPendingCount;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lab 6 integration test ("it"): the reminder logic is read-only, so running it twice gives the
 * same report and leaves the data untouched.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(PendingTranslationReminderService.class)
class PendingTranslationReminderServiceIntegrationTest {

    @Autowired
    private PendingTranslationReminderService reminderService;
    @Autowired
    private TranslationRepository translationRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private TextKeyRepository textKeyRepository;

    private Project busyProject;
    private Project quietProject;

    @BeforeEach
    void seed() {
        busyProject = project("Busy project");
        quietProject = project("Quiet project");
        Project allRejected = project("Only rejected");

        TextKey busyKey = textKey(busyProject);
        add(busyKey, TranslationStatus.PENDING);
        add(busyKey, TranslationStatus.PENDING);
        add(busyKey, TranslationStatus.PENDING);
        add(busyKey, TranslationStatus.APPROVED);

        add(textKey(quietProject), TranslationStatus.PENDING);
        add(textKey(allRejected), TranslationStatus.REJECTED);
    }

    @Test
    void onlyProjectsReachingTheThresholdAreReported() {
        List<ProjectPendingCount> atLeastTwo = reminderService.buildReminder(2);
        List<ProjectPendingCount> atLeastOne = reminderService.buildReminder(1);

        assertThat(atLeastTwo).extracting(ProjectPendingCount::projectName).containsExactly("Busy project");
        assertThat(atLeastTwo.get(0).pendingCount()).isEqualTo(3L); // the APPROVED one is not counted
        assertThat(atLeastOne).extracting(ProjectPendingCount::projectName)
                .containsExactly("Busy project", "Quiet project"); // "Only rejected" has no PENDING
    }

    @Test
    void runningTheReminderTwiceGivesTheSameReportAndChangesNothing() {
        long translationsBefore = translationRepository.count();

        List<ProjectPendingCount> first = reminderService.buildReminder(1);
        List<ProjectPendingCount> second = reminderService.buildReminder(1);

        assertThat(second).isEqualTo(first);
        assertThat(translationRepository.count()).isEqualTo(translationsBefore);
    }

    private Project project(String name) {
        Project project = new Project();
        project.setName(name);
        project.setSourceLanguage("en");
        project.setTargetLanguage("uk");
        return projectRepository.save(project);
    }

    private TextKey textKey(Project project) {
        TextKey key = new TextKey();
        key.setKeyName("key-of-" + project.getName());
        key.setSourceText("text");
        key.setProject(project);
        return textKeyRepository.save(key);
    }

    private void add(TextKey textKey, TranslationStatus status) {
        Translation translation = new Translation();
        translation.setTextKey(textKey);
        translation.setLanguageCode("uk");
        translation.setTranslatedText("текст");
        translation.setTranslatorName("tester");
        translation.setStatus(status);
        translationRepository.save(translation);
    }
}

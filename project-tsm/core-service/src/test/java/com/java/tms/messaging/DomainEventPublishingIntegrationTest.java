package com.java.tms.messaging;

import com.java.tms.common.event.ProjectCreatedEvent;
import com.java.tms.common.event.TranslationApprovedEvent;
import com.java.tms.common.event.TranslationProposedEvent;
import com.java.tms.domain.dto.ProjectCreateRequest;
import com.java.tms.domain.dto.ProjectResponse;
import com.java.tms.domain.dto.TranslationCreateRequest;
import com.java.tms.domain.dto.TranslationResponse;
import com.java.tms.domain.model.Project;
import com.java.tms.domain.model.TextKey;
import com.java.tms.domain.model.Translation;
import com.java.tms.repository.ProjectRepository;
import com.java.tms.repository.TextKeyRepository;
import com.java.tms.repository.TranslationRepository;
import com.java.tms.service.ProjectService;
import com.java.tms.service.TranslationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static com.java.tms.common.event.EventTopology.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Lab 8, scenarios 1 and 2 against the REAL Spring wiring (services + transaction manager + the
 * AFTER_COMMIT relay) with only the broker client replaced by a mock - no RabbitMQ is needed.
 *
 * <p>The test class is deliberately NOT {@code @Transactional}: the services must really commit (or
 * really roll back), otherwise the AFTER_COMMIT phase would never be reached and nothing could be
 * proven. The data is removed in {@link #cleanUp()}.
 */
@SpringBootTest(properties = "app.messaging.enabled=true")
@ActiveProfiles("test")
class DomainEventPublishingIntegrationTest {

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ProjectService projectService;
    @Autowired
    private TranslationService translationService;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private TextKeyRepository textKeyRepository;
    @Autowired
    private TranslationRepository translationRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanUp() {
        translationRepository.deleteAll();
        textKeyRepository.deleteAll();
        projectRepository.deleteAll();
    }

    @Test
    void committedProjectCreation_publishesExactlyOneEvent_afterCommit() {
        ProjectResponse created = projectService.create(
                new ProjectCreateRequest("Event project", "desc", "en", "uk"));

        ArgumentCaptor<ProjectCreatedEvent> captor = ArgumentCaptor.forClass(ProjectCreatedEvent.class);
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(EXCHANGE), eq(PROJECT_CREATED_KEY), captor.capture());
        ProjectCreatedEvent event = captor.getValue();
        assertThat(event.projectId()).isEqualTo(created.getId());
        assertThat(event.projectName()).isEqualTo("Event project");
        assertThat(projectRepository.existsById(created.getId())).isTrue(); // the row was committed
    }

    @Test
    void rolledBackProjectCreation_publishesNothing() {
        TransactionTemplate outerTransaction = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> outerTransaction.executeWithoutResult(status -> {
            projectService.create(new ProjectCreateRequest("Rolled back project", null, "en", "uk"));
            // the surrounding business operation fails AFTER create() succeeded -> rollback
            throw new IllegalStateException("simulated failure");
        })).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(rabbitTemplate);
        assertThat(projectRepository.findAll())
                .extracting(Project::getName).doesNotContain("Rolled back project");
    }

    @Test
    void failedProjectCreation_publishesNothing() {
        // missing required data -> the INSERT fails inside the service's own transaction
        assertThatThrownBy(() -> projectService.create(new ProjectCreateRequest(null, null, "en", "uk")))
                .isInstanceOf(RuntimeException.class);

        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void proposingTranslation_publishesTranslationProposedEvent() {
        Integer textKeyId = createTextKey();

        TranslationResponse proposed = translationService.propose(
                textKeyId, new TranslationCreateRequest("uk", "Привіт", null), "alice");

        ArgumentCaptor<TranslationProposedEvent> captor = ArgumentCaptor.forClass(TranslationProposedEvent.class);
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(EXCHANGE), eq(TRANSLATION_PROPOSED_KEY), captor.capture());
        assertThat(captor.getValue().translationId()).isEqualTo(proposed.getId());
        assertThat(captor.getValue().textKeyId()).isEqualTo(textKeyId);
        assertThat(captor.getValue().translatorName()).isEqualTo("alice");
    }

    @Test
    @WithMockUser(roles = "ADMIN") // approve() is protected by @PreAuthorize
    void approvingTranslation_publishesTranslationApprovedEvent() {
        Integer textKeyId = createTextKey();
        Translation translation = new Translation();
        translation.setTextKey(textKeyRepository.findById(textKeyId).orElseThrow());
        translation.setLanguageCode("uk");
        translation.setTranslatedText("Привіт");
        translation.setTranslatorName("alice");
        Integer translationId = translationRepository.save(translation).getId();

        translationService.approve(translationId);

        ArgumentCaptor<TranslationApprovedEvent> captor = ArgumentCaptor.forClass(TranslationApprovedEvent.class);
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(EXCHANGE), eq(TRANSLATION_APPROVED_KEY), captor.capture());
        assertThat(captor.getValue().translationId()).isEqualTo(translationId);
        assertThat(captor.getValue().textKeyId()).isEqualTo(textKeyId);
        assertThat(captor.getValue().translatorName()).isEqualTo("alice");
    }

    private Integer createTextKey() {
        Project project = new Project();
        project.setName("Event project for translations");
        project.setSourceLanguage("en");
        project.setTargetLanguage("uk");
        project = projectRepository.save(project);

        TextKey textKey = new TextKey();
        textKey.setKeyName("greeting");
        textKey.setSourceText("Hello");
        textKey.setProject(project);
        return textKeyRepository.save(textKey).getId();
    }
}

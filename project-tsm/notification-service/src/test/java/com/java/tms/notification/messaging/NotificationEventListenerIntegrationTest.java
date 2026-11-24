package com.java.tms.notification.messaging;

import com.java.tms.common.event.ProjectCreatedEvent;
import com.java.tms.common.event.TranslationApprovedEvent;
import com.java.tms.common.event.TranslationProposedEvent;
import com.java.tms.notification.model.Notification;
import com.java.tms.notification.model.NotificationType;
import com.java.tms.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lab 8, scenarios 1 and 5 with a real Spring context and a real (H2) database - only the broker
 * connection is switched off (listeners do not auto-start), the handler methods are called directly.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional // every test starts and ends with an empty notifications table
class NotificationEventListenerIntegrationTest {

    @Autowired
    private NotificationEventListener listener;
    @Autowired
    private NotificationRepository repository;

    @Test
    void receivingAnEvent_storesAVisibleRecord() {
        ProjectCreatedEvent event = ProjectCreatedEvent.of(1, "Limbus", "en", "uk");

        listener.onProjectCreated(event);

        List<Notification> all = repository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getType()).isEqualTo(NotificationType.PROJECT_CREATED);
        assertThat(all.get(0).getEventId()).isEqualTo(event.eventId().toString());
        assertThat(all.get(0).getMessage()).contains("Limbus");
    }

    @Test
    void sameEventAgain_leavesTheResultExactlyAsAfterTheFirstProcessing() {
        ProjectCreatedEvent event = ProjectCreatedEvent.of(1, "Limbus", "en", "uk");

        listener.onProjectCreated(event);
        List<Notification> afterFirst = repository.findAll();
        Integer firstId = afterFirst.get(0).getId();
        String firstMessage = afterFirst.get(0).getMessage();

        listener.onProjectCreated(event); // redelivery of the very same event
        listener.onProjectCreated(event);

        List<Notification> afterRepeats = repository.findAll();
        assertThat(afterRepeats).hasSize(1);
        assertThat(afterRepeats.get(0).getId()).isEqualTo(firstId);
        assertThat(afterRepeats.get(0).getMessage()).isEqualTo(firstMessage);
    }

    @Test
    void differentEvents_areEachStoredOnce() {
        listener.onProjectCreated(ProjectCreatedEvent.of(1, "A", "en", "uk"));
        listener.onTranslationProposed(TranslationProposedEvent.of(10, 5, "uk", "alice"));
        listener.onTranslationApproved(TranslationApprovedEvent.of(10, 5, "uk", "alice"));

        assertThat(repository.findAll()).extracting(Notification::getType)
                .containsExactlyInAnyOrder(NotificationType.PROJECT_CREATED,
                        NotificationType.TRANSLATION_PROPOSED, NotificationType.TRANSLATION_APPROVED);
    }
}

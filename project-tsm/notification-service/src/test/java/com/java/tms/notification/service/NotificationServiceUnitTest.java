package com.java.tms.notification.service;

import com.java.tms.common.event.ProjectCreatedEvent;
import com.java.tms.common.event.TranslationApprovedEvent;
import com.java.tms.common.event.TranslationProposedEvent;
import com.java.tms.notification.model.Notification;
import com.java.tms.notification.model.NotificationType;
import com.java.tms.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lab 8, scenario 5: handling the same event twice must leave the result unchanged. Pure Mockito, no
 * Spring context: the repository is a mock that "remembers" the event id after the first save.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceUnitTest {

    @Mock
    private NotificationRepository notificationRepository;
    @InjectMocks
    private NotificationService notificationService;

    @Test
    void handle_newProjectCreatedEvent_storesOneNotificationDescribingIt() {
        ProjectCreatedEvent event = ProjectCreatedEvent.of(1, "Limbus", "en", "uk");
        when(notificationRepository.existsByEventId(event.eventId().toString())).thenReturn(false);

        boolean stored = notificationService.handle(event);

        assertThat(stored).isTrue();
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getEventId()).isEqualTo(event.eventId().toString());
        assertThat(saved.getType()).isEqualTo(NotificationType.PROJECT_CREATED);
        assertThat(saved.getMessage()).contains("Limbus").contains("en").contains("uk");
        assertThat(saved.getOccurredAt()).isNotNull();
        assertThat(saved.getReceivedAt()).isNotNull();
    }

    @Test
    void handle_sameEventDeliveredTwice_isStoredOnlyOnce() {
        // AC5: the SAME event (same eventId) arrives again
        ProjectCreatedEvent event = ProjectCreatedEvent.of(1, "Limbus", "en", "uk");
        String id = event.eventId().toString();
        // first delivery: unknown id; second delivery: the id is already stored
        when(notificationRepository.existsByEventId(id)).thenReturn(false, true);

        boolean first = notificationService.handle(event);
        boolean second = notificationService.handle(event);

        assertThat(first).isTrue();
        assertThat(second).isFalse();
        verify(notificationRepository, times(1)).save(any(Notification.class)); // not twice
    }

    @Test
    void handle_alreadyProcessedTranslationEvents_neverSaveAgain() {
        TranslationProposedEvent proposed = TranslationProposedEvent.of(10, 5, "uk", "alice");
        TranslationApprovedEvent approved = TranslationApprovedEvent.of(10, 5, "uk", "alice");
        when(notificationRepository.existsByEventId(proposed.eventId().toString())).thenReturn(true);
        when(notificationRepository.existsByEventId(approved.eventId().toString())).thenReturn(true);

        assertThat(notificationService.handle(proposed)).isFalse();
        assertThat(notificationService.handle(approved)).isFalse();

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void handle_newTranslationEvents_describeWhoAndWhat() {
        TranslationProposedEvent proposed = TranslationProposedEvent.of(10, 5, "uk", "alice");
        TranslationApprovedEvent approved = TranslationApprovedEvent.of(10, 5, "uk", "alice");
        when(notificationRepository.existsByEventId(any())).thenReturn(false);

        notificationService.handle(proposed);
        notificationService.handle(approved);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Notification::getType)
                .containsExactly(NotificationType.TRANSLATION_PROPOSED, NotificationType.TRANSLATION_APPROVED);
        assertThat(captor.getAllValues()).allSatisfy(n -> assertThat(n.getMessage()).contains("alice"));
    }
}

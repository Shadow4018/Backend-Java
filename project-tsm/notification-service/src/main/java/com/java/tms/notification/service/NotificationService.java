package com.java.tms.notification.service;

import com.java.tms.common.event.ProjectCreatedEvent;
import com.java.tms.common.event.TranslationApprovedEvent;
import com.java.tms.common.event.TranslationProposedEvent;
import com.java.tms.notification.model.Notification;
import com.java.tms.notification.model.NotificationType;
import com.java.tms.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Lab 8: what the consumer DOES with an event - turns it into a stored notification.
 *
 * <p>Idempotent (scenario 5): the broker guarantees "at least once" delivery, so the same event may
 * arrive twice (redelivery after a crash, a retry...). Every handler first looks the event id up and
 * ignores an event it has already processed, so a second delivery changes nothing. Each method
 * returns {@code true} when the event was new and stored, {@code false} when it was a duplicate.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final ZoneId ZONE = ZoneId.of("Europe/Kyiv");

    private final NotificationRepository notificationRepository;

    @Transactional
    public boolean handle(ProjectCreatedEvent event) {
        String message = "Створено проєкт «%s» (%s → %s)".formatted(
                event.projectName(), event.sourceLanguage(), event.targetLanguage());
        return storeOnce(event.eventId(), event.occurredAt(), NotificationType.PROJECT_CREATED, message);
    }

    @Transactional
    public boolean handle(TranslationProposedEvent event) {
        String message = "Користувач %s запропонував переклад #%s (%s) для ключа #%s".formatted(
                event.translatorName(), event.translationId(), event.languageCode(), event.textKeyId());
        return storeOnce(event.eventId(), event.occurredAt(), NotificationType.TRANSLATION_PROPOSED, message);
    }

    @Transactional
    public boolean handle(TranslationApprovedEvent event) {
        String message = "Переклад #%s (%s) користувача %s затверджено".formatted(
                event.translationId(), event.languageCode(), event.translatorName());
        return storeOnce(event.eventId(), event.occurredAt(), NotificationType.TRANSLATION_APPROVED, message);
    }

    /** newest first */
    @Transactional(readOnly = true)
    public List<Notification> listAll() {
        return notificationRepository.findAllByOrderByIdDesc();
    }

    private boolean storeOnce(UUID eventId, Instant occurredAt, NotificationType type, String message) {
        String id = eventId.toString();
        if (notificationRepository.existsByEventId(id)) {
            log.info("Event {} ({}) was already processed - ignoring the duplicate", id, type);
            return false;
        }
        Notification notification = new Notification();
        notification.setEventId(id);
        notification.setType(type);
        notification.setMessage(message);
        notification.setOccurredAt(LocalDateTime.ofInstant(occurredAt, ZONE));
        notification.setReceivedAt(LocalDateTime.now(ZONE));
        notificationRepository.save(notification);
        log.info("Stored notification for event {} ({}): {}", id, type, message);
        return true;
    }
}

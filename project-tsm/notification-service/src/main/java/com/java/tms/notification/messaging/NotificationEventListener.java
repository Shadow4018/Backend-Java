package com.java.tms.notification.messaging;

import com.java.tms.common.event.ProjectCreatedEvent;
import com.java.tms.common.event.TranslationApprovedEvent;
import com.java.tms.common.event.TranslationProposedEvent;
import com.java.tms.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.java.tms.common.event.EventTopology.*;

/**
 * Lab 8: the entry point of the consumer. The method returns normally only after the notification
 * has been stored; only then does Spring AMQP acknowledge the message (ack mode AUTO). If the method
 * throws, the message is retried and finally dead-lettered, never silently lost.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = PROJECT_CREATED_QUEUE)
    public void onProjectCreated(ProjectCreatedEvent event) {
        log.info("Received ProjectCreatedEvent eventId={}", event.eventId());
        notificationService.handle(event);
    }

    @RabbitListener(queues = TRANSLATION_PROPOSED_QUEUE)
    public void onTranslationProposed(TranslationProposedEvent event) {
        log.info("Received TranslationProposedEvent eventId={}", event.eventId());
        notificationService.handle(event);
    }

    @RabbitListener(queues = TRANSLATION_APPROVED_QUEUE)
    public void onTranslationApproved(TranslationApprovedEvent event) {
        log.info("Received TranslationApprovedEvent eventId={}", event.eventId());
        notificationService.handle(event);
    }
}

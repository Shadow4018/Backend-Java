package com.java.tms.messaging;

import com.java.tms.common.event.DomainEvent;
import com.java.tms.common.event.ProjectCreatedEvent;
import com.java.tms.common.event.TranslationApprovedEvent;
import com.java.tms.common.event.TranslationProposedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.java.tms.common.event.EventTopology.*;

/**
 * Lab 8: the ONLY place where core-service talks to the broker.
 *
 * <p>Services do not call RabbitMQ directly. Inside their {@code @Transactional} method they publish a
 * plain Spring application event; this relay forwards it to RabbitMQ in the AFTER_COMMIT phase:
 * <ul>
 *   <li>if the business action fails or is rolled back, the transaction never commits, this listener is
 *       never invoked and NOTHING is published (scenario 2);</li>
 *   <li>if it commits, the event is published exactly once, after the data is really saved - the
 *       consumer can never see an event about a row that does not exist (yet).</li>
 * </ul>
 *
 * <p>Known limit (documented in the README): if the broker itself is down at that very moment, the
 * business action is already committed and the event cannot be sent. It is logged as an ERROR and the
 * HTTP request still succeeds. Closing this gap completely needs the transactional-outbox pattern.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class DomainEventRelay {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProjectCreated(ProjectCreatedEvent event) {
        publish(PROJECT_CREATED_KEY, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTranslationProposed(TranslationProposedEvent event) {
        publish(TRANSLATION_PROPOSED_KEY, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTranslationApproved(TranslationApprovedEvent event) {
        publish(TRANSLATION_APPROVED_KEY, event);
    }

    private void publish(String routingKey, DomainEvent event) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
            log.info("Published {} (eventId={}) to {} with key '{}'",
                    event.getClass().getSimpleName(), event.eventId(), EXCHANGE, routingKey);
        } catch (AmqpException ex) {
            log.error("Could not publish {} (eventId={}): broker unavailable? The business action itself "
                            + "was already committed.", event.getClass().getSimpleName(), event.eventId(), ex);
        }
    }
}

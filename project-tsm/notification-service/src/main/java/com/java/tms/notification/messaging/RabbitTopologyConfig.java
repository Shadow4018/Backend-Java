package com.java.tms.notification.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import static com.java.tms.common.event.EventTopology.*;

/**
 * Lab 8: the queues this service reads from. Declared idempotently on connect; the publisher declares
 * exactly the same objects (names come from the shared {@code EventTopology}), so whichever service
 * starts first creates them and the other one just finds them.
 *
 * <p>Every queue is DURABLE (survives a broker restart) and has a dead-letter queue: a message that
 * keeps failing is moved there after the retries instead of being dropped (scenario "data is not lost").
 */
@Configuration
public class RabbitTopologyConfig {

    @Bean
    public Declarables eventTopology() {
        TopicExchange events = new TopicExchange(EXCHANGE, true, false);
        DirectExchange deadLetters = new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);

        List<Declarable> declarables = new ArrayList<>();
        declarables.add(events);
        declarables.add(deadLetters);
        addQueue(declarables, events, deadLetters, PROJECT_CREATED_QUEUE, PROJECT_CREATED_KEY);
        addQueue(declarables, events, deadLetters, TRANSLATION_PROPOSED_QUEUE, TRANSLATION_PROPOSED_KEY);
        addQueue(declarables, events, deadLetters, TRANSLATION_APPROVED_QUEUE, TRANSLATION_APPROVED_KEY);
        return new Declarables(declarables);
    }

    private static void addQueue(List<Declarable> declarables, TopicExchange events, DirectExchange deadLetters,
                                 String queueName, String routingKey) {
        String dlqName = queueName + DLQ_SUFFIX;

        Queue queue = QueueBuilder.durable(queueName)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(dlqName)
                .build();
        Queue dlq = QueueBuilder.durable(dlqName).build();

        Binding main = BindingBuilder.bind(queue).to(events).with(routingKey);
        Binding dead = BindingBuilder.bind(dlq).to(deadLetters).with(dlqName);

        declarables.add(queue);
        declarables.add(dlq);
        declarables.add(main);
        declarables.add(dead);
    }
}

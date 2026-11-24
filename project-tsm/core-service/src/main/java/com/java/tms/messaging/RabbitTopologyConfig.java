package com.java.tms.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import static com.java.tms.common.event.EventTopology.*;

/**
 * Lab 8: declares the broker objects the publisher needs. The declaration is idempotent: Spring AMQP
 * (RabbitAdmin) creates them on the first connection and does nothing if they already exist.
 *
 * <p>The publisher declares the consumer's QUEUES too (not only the exchange). That is deliberate:
 * a message sent to an exchange that has no bound queue is silently dropped, so if the consumer has
 * never started, events published meanwhile would be lost. With the queue declared here, they wait in
 * the queue until the consumer starts (scenario 4). The consumer declares exactly the same objects.
 */
@Configuration
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitTopologyConfig {

    @Bean
    public Declarables eventTopology() {
        TopicExchange events = new TopicExchange(EXCHANGE, true, false);
        DirectExchange deadLetters = new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);

        List<org.springframework.amqp.core.Declarable> declarables = new ArrayList<>();
        declarables.add(events);
        declarables.add(deadLetters);
        addConsumerQueue(declarables, events, deadLetters, PROJECT_CREATED_QUEUE, PROJECT_CREATED_KEY);
        addConsumerQueue(declarables, events, deadLetters, TRANSLATION_PROPOSED_QUEUE, TRANSLATION_PROPOSED_KEY);
        addConsumerQueue(declarables, events, deadLetters, TRANSLATION_APPROVED_QUEUE, TRANSLATION_APPROVED_KEY);
        return new Declarables(declarables);
    }

    private static void addConsumerQueue(List<org.springframework.amqp.core.Declarable> declarables,
                                         TopicExchange events, DirectExchange deadLetters,
                                         String queueName, String routingKey) {
        String dlqName = queueName + DLQ_SUFFIX;

        // durable + dead-lettering: a rejected message goes to the DLQ instead of disappearing
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

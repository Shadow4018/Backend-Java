package com.java.tms.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.java.tms.common.event.ProjectCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import java.io.IOException;

import static com.java.tms.common.event.EventTopology.EXCHANGE;
import static com.java.tms.common.event.EventTopology.PROJECT_CREATED_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DomainEventRelayTest {

    @Test
    void afterCommit_sendsTheEventToTheExchangeWithItsRoutingKey() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        DomainEventRelay relay = new DomainEventRelay(rabbitTemplate);
        ProjectCreatedEvent event = ProjectCreatedEvent.of(1, "P", "en", "uk");

        relay.onProjectCreated(event);

        verify(rabbitTemplate).convertAndSend(EXCHANGE, PROJECT_CREATED_KEY, (Object) event);
    }

    @Test
    void brokerDown_isLoggedNotThrown_soTheCommittedBusinessActionStillSucceeds() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        doThrow(new AmqpConnectException(new IOException("Connection refused")))
                .when(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(PROJECT_CREATED_KEY), any(Object.class));
        DomainEventRelay relay = new DomainEventRelay(rabbitTemplate);

        assertThatCode(() -> relay.onProjectCreated(ProjectCreatedEvent.of(1, "P", "en", "uk")))
                .doesNotThrowAnyException();
    }

    @Test
    void jsonConverter_writesAReadablePersistentMessage() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        Jackson2JsonMessageConverter converter =
                (Jackson2JsonMessageConverter) new MessagingConfig().jsonMessageConverter(objectMapper);
        ProjectCreatedEvent event = ProjectCreatedEvent.of(7, "Limbus", "en", "uk");

        Message message = converter.toMessage(event, new MessageProperties());
        String json = new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8);

        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
        // persistent => survives a broker restart while it waits for a stopped consumer
        assertThat(message.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
        assertThat(json).contains("\"projectName\":\"Limbus\"").contains(event.eventId().toString());
    }
}

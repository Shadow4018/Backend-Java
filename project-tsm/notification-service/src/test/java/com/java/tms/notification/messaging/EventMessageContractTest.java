package com.java.tms.notification.messaging;

import com.java.tms.common.event.ProjectCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The wire contract: what the publisher serialises, the consumer's converter reads back unchanged.
 * A garbled message is a conversion error, which Spring AMQP treats as non-retryable and dead-letters.
 */
@SpringBootTest
@ActiveProfiles("test")
class EventMessageContractTest {

    @Autowired
    private MessageConverter converter;

    @Test
    void eventSurvivesTheJsonRoundTrip() {
        ProjectCreatedEvent event = ProjectCreatedEvent.of(7, "Limbus", "en", "uk");

        Message message = converter.toMessage(event, new MessageProperties());
        Object restored = converter.fromMessage(message);

        assertThat(restored).isEqualTo(event);
    }

    @Test
    void garbledBody_isAConversionError() {
        MessageProperties properties = new MessageProperties();
        properties.setContentType("application/json");
        properties.setHeader("__TypeId__", ProjectCreatedEvent.class.getName());
        Message message = new Message("{ this is not json".getBytes(StandardCharsets.UTF_8), properties);

        assertThatThrownBy(() -> converter.fromMessage(message)).isInstanceOf(MessageConversionException.class);
    }

    @Test
    void classOutsideTheTrustedPackages_isRejected() {
        MessageProperties properties = new MessageProperties();
        properties.setContentType("application/json");
        properties.setHeader("__TypeId__", "com.evil.Payload"); // not a class of tms-common
        Message message = new Message("{}".getBytes(StandardCharsets.UTF_8), properties);

        assertThatThrownBy(() -> converter.fromMessage(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trusted packages");
    }
}

package com.java.tms.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Lab 8: events travel as JSON (readable in the RabbitMQ UI, no Java serialization). Spring Boot
 * picks this {@link MessageConverter} up for its auto-configured {@code RabbitTemplate}.
 *
 * <p>The application's own {@link ObjectMapper} is reused, so {@code Instant}/{@code UUID} are
 * written as ISO-8601 text / strings exactly like in the REST API.
 */
@Configuration
public class MessagingConfig {

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}

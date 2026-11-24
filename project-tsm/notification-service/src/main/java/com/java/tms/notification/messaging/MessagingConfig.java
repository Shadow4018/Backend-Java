package com.java.tms.notification.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Lab 8: reads the JSON messages written by the publisher back into the shared event DTOs.
 * Only the package of those DTOs is trusted for deserialization, so a message cannot make the
 * consumer instantiate an arbitrary class.
 */
@Configuration
public class MessagingConfig {

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("com.java.tms.common.event");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}

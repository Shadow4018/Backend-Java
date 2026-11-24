package com.java.tms.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** The context (JPA, Liquibase on H2, AMQP beans, listeners) starts without a broker. */
@SpringBootTest
@ActiveProfiles("test")
class NotificationApplicationTest {

    @Test
    void contextLoads() {
    }
}

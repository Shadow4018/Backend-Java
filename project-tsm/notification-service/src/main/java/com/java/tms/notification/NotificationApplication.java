package com.java.tms.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Lab 8: the CONSUMER service. It never calls core-service (no HTTP client at all) and has no
 * class of core-service on its classpath: it only listens to a RabbitMQ queue and stores what it hears.
 */
@SpringBootApplication
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}

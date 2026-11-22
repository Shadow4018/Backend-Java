package com.java.tms.glossary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starts the glossary service (port 8082): the terminology glossary as an independent service
 * with its own database, its own security configuration and its own tests.
 */
@SpringBootApplication
public class GlossaryApplication {

    public static void main(String[] args) {
        SpringApplication.run(GlossaryApplication.class, args);
    }
}

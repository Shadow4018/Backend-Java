package com.java.tms.common.event;

import java.time.Instant;
import java.util.UUID;

/** Business action 1: a translation project was created (and the transaction committed). */
public record ProjectCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        Integer projectId,
        String projectName,
        String sourceLanguage,
        String targetLanguage) implements DomainEvent {

    public static ProjectCreatedEvent of(Integer projectId, String projectName,
                                         String sourceLanguage, String targetLanguage) {
        return new ProjectCreatedEvent(UUID.randomUUID(), Instant.now(),
                projectId, projectName, sourceLanguage, targetLanguage);
    }
}

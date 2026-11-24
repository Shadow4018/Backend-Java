package com.java.tms.common.event;

import java.time.Instant;
import java.util.UUID;

/** Business action 3: an editor approved a translation proposal. */
public record TranslationApprovedEvent(
        UUID eventId,
        Instant occurredAt,
        Integer translationId,
        Integer textKeyId,
        String languageCode,
        String translatorName) implements DomainEvent {

    public static TranslationApprovedEvent of(Integer translationId, Integer textKeyId,
                                              String languageCode, String translatorName) {
        return new TranslationApprovedEvent(UUID.randomUUID(), Instant.now(),
                translationId, textKeyId, languageCode, translatorName);
    }
}

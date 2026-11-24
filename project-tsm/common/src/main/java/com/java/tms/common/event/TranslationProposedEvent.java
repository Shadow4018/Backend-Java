package com.java.tms.common.event;

import java.time.Instant;
import java.util.UUID;

/** Business action 2: a translator proposed a translation for a text key. */
public record TranslationProposedEvent(
        UUID eventId,
        Instant occurredAt,
        Integer translationId,
        Integer textKeyId,
        String languageCode,
        String translatorName) implements DomainEvent {

    public static TranslationProposedEvent of(Integer translationId, Integer textKeyId,
                                              String languageCode, String translatorName) {
        return new TranslationProposedEvent(UUID.randomUUID(), Instant.now(),
                translationId, textKeyId, languageCode, translatorName);
    }
}

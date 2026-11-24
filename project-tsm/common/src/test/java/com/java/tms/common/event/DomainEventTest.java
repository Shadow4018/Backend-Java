package com.java.tms.common.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEventTest {

    @Test
    void factory_assignsFreshEventIdAndTimestamp_eachTime() {
        ProjectCreatedEvent first = ProjectCreatedEvent.of(1, "P", "en", "uk");
        ProjectCreatedEvent second = ProjectCreatedEvent.of(1, "P", "en", "uk");

        assertThat(first.eventId()).isNotNull().isNotEqualTo(second.eventId());
        assertThat(first.occurredAt()).isNotNull();
        assertThat(first.projectName()).isEqualTo("P");
    }

    @Test
    void translationEvents_carryTheFieldsTheConsumerNeeds() {
        TranslationProposedEvent proposed = TranslationProposedEvent.of(10, 5, "uk", "alice");
        TranslationApprovedEvent approved = TranslationApprovedEvent.of(10, 5, "uk", "alice");

        assertThat(proposed.translationId()).isEqualTo(10);
        assertThat(proposed.translatorName()).isEqualTo("alice");
        assertThat(approved.textKeyId()).isEqualTo(5);
        assertThat(approved.eventId()).isNotEqualTo(proposed.eventId());
    }
}

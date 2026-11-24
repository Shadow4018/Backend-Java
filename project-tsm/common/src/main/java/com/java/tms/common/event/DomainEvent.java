package com.java.tms.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Lab 8: contract of every event that travels through the message broker.
 *
 * <p>Lives in {@code tms-common} because the producer and the consumer must agree on the shape of
 * the message (the only thing the lab allows two services to share). It is a plain data contract:
 * no Spring, no AMQP and no behaviour.
 *
 * <p>{@link #eventId()} is generated ONCE, when the business action happens, and is never
 * regenerated on redelivery. The consumer uses it to recognise a duplicate (idempotency).
 */
public interface DomainEvent {

    UUID eventId();

    Instant occurredAt();
}

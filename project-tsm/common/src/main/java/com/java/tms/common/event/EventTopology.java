package com.java.tms.common.event;

/**
 * Lab 8: names of the broker objects (exchange, routing keys, queues). They are a contract
 * between the publisher and the consumer, so they are defined once, here, instead of as string
 * literals duplicated in two services.
 *
 * <p>Layout: the publisher sends to the topic exchange {@link #EXCHANGE} with one routing key per
 * event type; every consumer owns a durable queue bound to the keys it cares about. A queue that
 * rejects a message (after the retries) forwards it to {@link #DEAD_LETTER_EXCHANGE}, where it
 * lands in "&lt;queue&gt;.dlq" instead of being lost.
 */
public final class EventTopology {

    public static final String EXCHANGE = "tms.events";
    public static final String DEAD_LETTER_EXCHANGE = "tms.events.dlx";
    public static final String DLQ_SUFFIX = ".dlq";

    public static final String PROJECT_CREATED_KEY = "project.created";
    public static final String TRANSLATION_PROPOSED_KEY = "translation.proposed";
    public static final String TRANSLATION_APPROVED_KEY = "translation.approved";

    public static final String PROJECT_CREATED_QUEUE = "notification.project-created";
    public static final String TRANSLATION_PROPOSED_QUEUE = "notification.translation-proposed";
    public static final String TRANSLATION_APPROVED_QUEUE = "notification.translation-approved";

    private EventTopology() {
    }
}

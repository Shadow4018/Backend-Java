package com.java.tms.notification.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * The visible RESULT of handling an event (Lab 8, scenario 1): one row per distinct event.
 * {@code eventId} is unique - that is what makes handling idempotent.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** id of the event in the publisher (never changes on redelivery) */
    @Column(nullable = false, unique = true, length = 36)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 500)
    private String message;

    /** when the business action happened in the publisher */
    @Column(nullable = false)
    private LocalDateTime occurredAt;

    /** when THIS service processed the event (later than occurredAt if the consumer was down) */
    @Column(nullable = false)
    private LocalDateTime receivedAt;
}

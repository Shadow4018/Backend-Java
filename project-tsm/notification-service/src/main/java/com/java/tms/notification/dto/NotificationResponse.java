package com.java.tms.notification.dto;

import com.java.tms.notification.model.Notification;
import com.java.tms.notification.model.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Integer id,
        String eventId,
        NotificationType type,
        String message,
        LocalDateTime occurredAt,
        LocalDateTime receivedAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getEventId(), n.getType(), n.getMessage(),
                n.getOccurredAt(), n.getReceivedAt());
    }
}

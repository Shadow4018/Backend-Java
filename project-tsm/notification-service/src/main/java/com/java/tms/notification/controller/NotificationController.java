package com.java.tms.notification.controller;

import com.java.tms.notification.dto.NotificationResponse;
import com.java.tms.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lab 8, scenario 1: "the result becomes visible through the corresponding endpoint".
 * Read-only; notifications are created only by events from the broker, never through HTTP.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> list() {
        return notificationService.listAll().stream().map(NotificationResponse::from).toList();
    }
}

package com.java.tms.notification.controller;

import com.java.tms.common.event.ProjectCreatedEvent;
import com.java.tms.notification.messaging.NotificationEventListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Lab 8, scenario 1: the processed event is visible through the endpoint. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private NotificationEventListener listener;

    @Test
    void noEventsYet_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void afterAnEvent_theNotificationAppearsWithTypeAndMessage() throws Exception {
        ProjectCreatedEvent event = ProjectCreatedEvent.of(1, "Limbus", "en", "uk");

        listener.onProjectCreated(event);

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("PROJECT_CREATED"))
                .andExpect(jsonPath("$[0].eventId").value(event.eventId().toString()))
                .andExpect(jsonPath("$[0].message").value(org.hamcrest.Matchers.containsString("Limbus")));
    }

    @Test
    void newestNotificationComesFirst() throws Exception {
        listener.onProjectCreated(ProjectCreatedEvent.of(1, "First", "en", "uk"));
        listener.onProjectCreated(ProjectCreatedEvent.of(2, "Second", "en", "uk"));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(jsonPath("$[0].message").value(org.hamcrest.Matchers.containsString("Second")))
                .andExpect(jsonPath("$[1].message").value(org.hamcrest.Matchers.containsString("First")));
    }
}

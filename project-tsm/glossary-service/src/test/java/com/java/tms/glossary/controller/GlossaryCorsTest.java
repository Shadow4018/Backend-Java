package com.java.tms.glossary.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The UI is served by core-service (8081) and calls this service (8082) from the browser, so
 * the browser first sends a CORS preflight. It must succeed for exactly that origin - and only it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlossaryCorsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void preflightFromTheCoreServiceUiOriginIsAllowed() throws Exception {
        mockMvc.perform(options("/api/glossary")
                        .header("Origin", "http://localhost:8081")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"));
    }

    @Test
    void preflightFromAnyOtherOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/glossary")
                        .header("Origin", "http://evil.example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}

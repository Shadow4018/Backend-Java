package com.java.tms.glossary.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The glossary service on its own (own Spring context, own H2 database, no other service
 * running): reading is public, changing is ADMIN-only, and errors use the shared error body.
 *
 * <p>Note: the jwt() post-processor does not use our custom converter, so roles are passed
 * explicitly via authorities(...); the converter is covered by its own test below.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlossarySecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    private static String body(String term) {
        return "{\"term\":\"" + term + "\",\"translation\":\"translation of " + term + "\"}";
    }

    @Test
    void listingTheGlossaryIsPublic() throws Exception {
        mockMvc.perform(get("/api/glossary"))
                .andExpect(status().isOk());
    }

    @Test
    void addingATermWithoutATokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/glossary").contentType("application/json").content(body("no-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addingATermAsANonAdminIsForbidden() throws Exception {
        mockMvc.perform(post("/api/glossary")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRANSLATOR")))
                        .contentType("application/json").content(body("translator-term")))
                .andExpect(status().isForbidden());
    }

    @Test
    void addingATermAsAdminReturns201AndTheStoredTerm() throws Exception {
        mockMvc.perform(post("/api/glossary")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType("application/json").content(body("admin-term")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.term").value("admin-term"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void aBlankTermIsRejectedWith400() throws Exception {
        mockMvc.perform(post("/api/glossary")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType("application/json").content("{\"term\":\"\",\"translation\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400)); // body type shared through the "common" module
    }

    @Test
    void addingTheSameTermTwiceIsAConflictNotAServerError() throws Exception {
        var admin = jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));

        mockMvc.perform(post("/api/glossary").with(admin).contentType("application/json").content(body("duplicate-term")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/glossary").with(admin).contentType("application/json").content(body("duplicate-term")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void deletingAnUnknownTermIsReportedAs400WithTheMessage() throws Exception {
        mockMvc.perform(delete("/api/glossary/987654")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("GlossaryTerm not found: 987654"));
    }

    @Test
    void keycloakRealmRolesAreMappedToAuthorities() {
        Jwt token = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("preferred_username", "belialu")
                .claim("realm_access", Map.of("roles", List.of("USER", "ADMIN")))
                .build();

        var authorities = jwtAuthenticationConverter.convert(token).getAuthorities();

        assertThat(authorities).extracting(Object::toString).contains("ROLE_ADMIN", "ROLE_USER");
    }
}

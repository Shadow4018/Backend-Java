package com.java.tms.controller;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AC6: access restriction at SecurityFilterChain level (creating a project is ADMIN-only).
 * Also checks that Keycloak's realm_access.roles claim is turned into ROLE_* authorities.
 *
 * <p>Note: the jwt() post-processor does NOT use our custom converter, so roles are passed
 * explicitly via authorities(...); the converter itself is covered by a separate test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectSecurityTest {

    private static final String NEW_PROJECT = "{\"name\":\"Admin Project\",\"sourceLanguage\":\"en\",\"targetLanguage\":\"uk\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Test
    void listingProjectsIsPublic() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk());
    }

    @Test
    void creatingProjectWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType("application/json")
                        .content(NEW_PROJECT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void creatingProjectAsNonAdminIsForbidden() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRANSLATOR")))
                        .contentType("application/json")
                        .content(NEW_PROJECT))
                .andExpect(status().isForbidden());
    }

    @Test
    void creatingProjectAsAdminSucceeds() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType("application/json")
                        .content(NEW_PROJECT))
                .andExpect(status().isCreated());
    }

    @Test
    void keycloakRealmRolesAreMappedToAuthorities() {
        Jwt token = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("preferred_username", "belialu")
                .claim("realm_access", Map.of("roles", List.of("USER", "ADMIN")))
                .build();

        List<String> authorities = jwtAuthenticationConverter.convert(token).getAuthorities().stream()
                .map(Object::toString)
                .toList();

        assertThat(authorities).contains("ROLE_ADMIN", "ROLE_USER");
    }
}

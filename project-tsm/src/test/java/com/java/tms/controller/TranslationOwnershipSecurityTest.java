package com.java.tms.controller;

import com.java.tms.domain.model.Project;
import com.java.tms.domain.model.TextKey;
import com.java.tms.domain.model.Translation;
import com.java.tms.repository.ProjectRepository;
import com.java.tms.repository.TextKeyRepository;
import com.java.tms.repository.TranslationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A translator may edit/delete only their OWN pending proposal; this is enforced in
 * TranslationService by comparing translatorName to the caller's username (a JWT "sub"
 * claim here), not by role alone — a different TRANSLATOR must get 403.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TranslationOwnershipSecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private TextKeyRepository textKeyRepository;
    @Autowired
    private TranslationRepository translationRepository;

    private Integer textKeyId;
    private Integer translationId;

    @BeforeEach
    void createOwnedTranslation() {
        Project project = new Project();
        project.setName("Ownership test project");
        project.setSourceLanguage("en");
        project.setTargetLanguage("uk");
        project = projectRepository.save(project);

        TextKey textKey = new TextKey();
        textKey.setKeyName("greeting");
        textKey.setSourceText("Hello");
        textKey.setProject(project);
        textKey = textKeyRepository.save(textKey);
        textKeyId = textKey.getId();

        Translation translation = new Translation();
        translation.setTextKey(textKey);
        translation.setLanguageCode("uk");
        translation.setTranslatedText("Привіт");
        translation.setTranslatorName("owner-translator");
        translationId = translationRepository.save(translation).getId();
    }

    @Test
    void ownerCanEditTheirOwnPendingProposal() throws Exception {
        mockMvc.perform(put("/api/text-keys/{textKeyId}/translations/{id}", textKeyId, translationId)
                        .with(jwt().jwt(j -> j.subject("owner-translator")
                                        .claim("preferred_username", "owner-translator"))
                                .authorities(new SimpleGrantedAuthority("ROLE_TRANSLATOR")))
                        .contentType("application/json")
                        .content("{\"languageCode\":\"uk\",\"translatedText\":\"Привіт, виправлено\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translatedText").value("Привіт, виправлено"));
    }

    @Test
    void anotherTranslatorCannotEditSomeoneElsesProposal() throws Exception {
        mockMvc.perform(put("/api/text-keys/{textKeyId}/translations/{id}", textKeyId, translationId)
                        .with(jwt().jwt(j -> j.subject("someone-else")
                                        .claim("preferred_username", "someone-else"))
                                .authorities(new SimpleGrantedAuthority("ROLE_TRANSLATOR")))
                        .contentType("application/json")
                        .content("{\"languageCode\":\"uk\",\"translatedText\":\"Чужий переклад\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanEditAnyonesProposal() throws Exception {
        mockMvc.perform(put("/api/text-keys/{textKeyId}/translations/{id}", textKeyId, translationId)
                        .with(jwt().jwt(j -> j.claim("preferred_username", "belialu"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType("application/json")
                        .content("{\"languageCode\":\"uk\",\"translatedText\":\"Відредаговано адміном\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void ownerCanDeleteTheirOwnProposal() throws Exception {
        mockMvc.perform(delete("/api/translations/{id}", translationId)
                        .with(jwt().jwt(j -> j.claim("preferred_username", "owner-translator"))
                                .authorities(new SimpleGrantedAuthority("ROLE_TRANSLATOR"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void anotherTranslatorCannotDeleteSomeoneElsesProposal() throws Exception {
        mockMvc.perform(delete("/api/translations/{id}", translationId)
                        .with(jwt().jwt(j -> j.claim("preferred_username", "someone-else"))
                                .authorities(new SimpleGrantedAuthority("ROLE_TRANSLATOR"))))
                .andExpect(status().isForbidden());
    }
}

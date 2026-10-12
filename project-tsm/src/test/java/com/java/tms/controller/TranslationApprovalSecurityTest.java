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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AC7: approving a translation is restricted by @PreAuthorize on TranslationService#approve.
 * The SecurityFilterChain only requires an authenticated user for this path, so a 403 for a
 * TRANSLATOR can only come from method security (AccessDeniedException).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TranslationApprovalSecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private TextKeyRepository textKeyRepository;
    @Autowired
    private TranslationRepository translationRepository;

    private Integer translationId;

    @BeforeEach
    void createTranslation() {
        Project project = new Project();
        project.setName("Approval test project");
        project.setSourceLanguage("en");
        project.setTargetLanguage("uk");
        project = projectRepository.save(project);

        TextKey textKey = new TextKey();
        textKey.setKeyName("greeting");
        textKey.setSourceText("Hello");
        textKey.setProject(project);
        textKey = textKeyRepository.save(textKey);

        Translation translation = new Translation();
        translation.setTextKey(textKey);
        translation.setLanguageCode("uk");
        translation.setTranslatedText("Привіт");
        translation.setTranslatorName("translator");
        translationId = translationRepository.save(translation).getId();
    }

    @Test
    void approveWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/translations/{id}/approve", translationId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void approveAsTranslatorIsBlockedByPreAuthorize() throws Exception {
        mockMvc.perform(patch("/api/translations/{id}/approve", translationId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRANSLATOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void approveAsEditorSucceeds() throws Exception {
        mockMvc.perform(patch("/api/translations/{id}/approve", translationId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EDITOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }
}

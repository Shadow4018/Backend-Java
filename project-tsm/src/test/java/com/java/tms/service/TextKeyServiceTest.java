package com.java.tms.service;

import com.java.tms.domain.dto.ProjectCreateRequest;
import com.java.tms.domain.dto.TextKeyCreateRequest;
import com.java.tms.domain.dto.TextKeyResponse;
import com.java.tms.domain.mapper.ProjectMapperImpl;
import com.java.tms.domain.mapper.TextKeyMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the "number" shown next to a text key is a per-project display position,
 * NOT the entity's real (globally auto-incrementing) id: a second project's first text key
 * is numbered 1 again, even though its database id keeps counting up from the first project.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({ProjectMapperImpl.class, TextKeyMapperImpl.class, ProjectService.class, TextKeyService.class})
class TextKeyServiceTest {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private TextKeyService textKeyService;

    @Test
    void numberingRestartsForEachProject() {
        Integer projectA = projectService.create(projectRequest("Project A")).getId();
        textKeyService.create(projectA, textKeyRequest("a.greeting"));
        textKeyService.create(projectA, textKeyRequest("a.farewell"));

        Integer projectB = projectService.create(projectRequest("Project B")).getId();
        TextKeyResponse firstKeyOfProjectB = textKeyService.create(projectB, textKeyRequest("b.greeting"));

        List<TextKeyResponse> projectAKeys = textKeyService.listByProject(projectA);
        List<TextKeyResponse> projectBKeys = textKeyService.listByProject(projectB);

        assertThat(projectAKeys).extracting(TextKeyResponse::getNumber).containsExactly(1, 2);
        assertThat(projectBKeys).extracting(TextKeyResponse::getNumber).containsExactly(1);

        // The real id is never reused/reset across projects — only "number" is per-project.
        // Note: create() itself never sets "number" (only listByProject() assigns it), so we
        // compare the real id here, and check "number" only through the listing above.
        assertThat(firstKeyOfProjectB.getId()).isGreaterThan(projectAKeys.get(1).getId());
    }

    private ProjectCreateRequest projectRequest(String name) {
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setName(name);
        request.setSourceLanguage("en");
        request.setTargetLanguage("uk");
        return request;
    }

    private TextKeyCreateRequest textKeyRequest(String keyName) {
        TextKeyCreateRequest request = new TextKeyCreateRequest();
        request.setKeyName(keyName);
        request.setSourceText("Sample text for " + keyName);
        return request;
    }
}

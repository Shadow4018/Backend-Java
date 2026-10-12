package com.java.tms.service;

import com.java.tms.domain.dto.ProjectCreateRequest;
import com.java.tms.domain.dto.ProjectResponse;
import com.java.tms.domain.dto.ProjectUpdateRequest;
import com.java.tms.domain.enums.ProjectStatus;
import com.java.tms.domain.mapper.ProjectMapperImpl;
import com.java.tms.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// "test" profile disables the "dev" Liquibase context, so the seed data
// (one demo project) is NOT inserted and counts below stay exact.
@DataJpaTest
@ActiveProfiles("test")
@Import({ProjectMapperImpl.class, ProjectService.class})
class ProjectServiceTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void shouldCreateProject() {
        ProjectResponse response = projectService.create(sampleRequest());

        assertThat(response.getName()).isEqualTo("Game Localization");
        assertThat(response.getTargetLanguage()).isEqualTo("uk");
        assertThat(response.getStatus()).isEqualTo(ProjectStatus.DRAFT);
        assertThat(projectRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldUpdateProject() {
        ProjectResponse created = projectService.create(sampleRequest());

        ProjectUpdateRequest update = new ProjectUpdateRequest();
        update.setName("Game Localization v2");
        update.setSourceLanguage("en");
        update.setTargetLanguage("pl");
        update.setStatus(ProjectStatus.ACTIVE);

        ProjectResponse updated = projectService.update(created.getId(), update);

        assertThat(updated.getName()).isEqualTo("Game Localization v2");
        assertThat(updated.getTargetLanguage()).isEqualTo("pl");
        assertThat(updated.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    void shouldDeleteProject() {
        ProjectResponse created = projectService.create(sampleRequest());

        projectService.delete(created.getId());

        assertThat(projectRepository.count()).isZero();
    }

    @Test
    void shouldThrowWhenProjectNotFound() {
        assertThatThrownBy(() -> projectService.getById(999))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ProjectCreateRequest sampleRequest() {
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setName("Game Localization");
        request.setSourceLanguage("en");
        request.setTargetLanguage("uk");
        return request;
    }
}

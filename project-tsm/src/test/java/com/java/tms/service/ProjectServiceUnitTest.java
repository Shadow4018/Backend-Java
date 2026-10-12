package com.java.tms.service;

import com.java.tms.domain.dto.ProjectCreateRequest;
import com.java.tms.domain.dto.ProjectResponse;
import com.java.tms.domain.dto.ProjectUpdateRequest;
import com.java.tms.domain.enums.ProjectStatus;
import com.java.tms.domain.mapper.ProjectMapper;
import com.java.tms.domain.model.Project;
import com.java.tms.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pure Mockito unit tests for {@link ProjectService} — Lab 2 (Unit Testing).
 *
 * <p>No Spring context, no real database: both collaborators ({@link ProjectRepository},
 * {@link ProjectMapper}) are mocked with {@code @Mock} and injected via {@code @InjectMocks}
 * (AC3). Every public method of {@link ProjectService} has at least one "happy path" test
 * (AC1) and one invalid/edge-case test (AC2), satisfying AC8 for this class.
 */
@ExtendWith(MockitoExtension.class)
class ProjectServiceUnitTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectMapper projectMapper;
    @InjectMocks
    private ProjectService projectService;

    @Test
    void create_validRequest_savesEntityBuiltFromMapperAndReturnsMappedResponse() {
        // Arrange
        ProjectCreateRequest request = new ProjectCreateRequest("Limbus Localization", "desc", "en", "uk");
        Project entityFromMapper = new Project(); // what the mapper hands back for the incoming request
        entityFromMapper.setName("Limbus Localization");
        Project savedEntity = new Project();
        savedEntity.setId(1);
        savedEntity.setName("Limbus Localization");
        ProjectResponse expectedResponse = new ProjectResponse(1, "Limbus Localization", "desc", "en", "uk", ProjectStatus.DRAFT);

        when(projectMapper.toEntity(request)).thenReturn(entityFromMapper);
        when(projectRepository.save(entityFromMapper)).thenReturn(savedEntity);
        when(projectMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

        // Act
        ProjectResponse result = projectService.create(request);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        ArgumentCaptor<Project> savedCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getName()).isEqualTo("Limbus Localization");
    }

    @Test
    void create_repositoryRejectsSave_exceptionPropagatesUnwrapped() {
        // Arrange: simulate a DB-level failure (e.g. unique constraint violation on name)
        ProjectCreateRequest request = new ProjectCreateRequest("Duplicate", null, "en", "uk");
        Project entityFromMapper = new Project();
        when(projectMapper.toEntity(request)).thenReturn(entityFromMapper);
        when(projectRepository.save(entityFromMapper))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate name"));

        // Act + Assert: the service does not swallow the failure or convert it silently
        assertThatThrownBy(() -> projectService.create(request))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        verify(projectMapper, never()).toResponse(any());
    }

    @Test
    void listAll_whenProjectsExist_returnsResponsesInRepositoryOrder() {
        // Arrange
        Project first = new Project();
        first.setId(1);
        Project second = new Project();
        second.setId(2);
        ProjectResponse firstResponse = new ProjectResponse(1, "A", null, "en", "uk", ProjectStatus.DRAFT);
        ProjectResponse secondResponse = new ProjectResponse(2, "B", null, "en", "pl", ProjectStatus.ACTIVE);

        when(projectRepository.findAll()).thenReturn(List.of(first, second));
        when(projectMapper.toResponse(first)).thenReturn(firstResponse);
        when(projectMapper.toResponse(second)).thenReturn(secondResponse);

        // Act
        List<ProjectResponse> result = projectService.listAll();

        // Assert
        assertThat(result).containsExactly(firstResponse, secondResponse);
    }

    @Test
    void listAll_whenNoProjectsExist_returnsEmptyListWithoutMapperCalls() {
        // Arrange
        when(projectRepository.findAll()).thenReturn(List.of());

        // Act
        List<ProjectResponse> result = projectService.listAll();

        // Assert
        assertThat(result).isEmpty();
        verifyNoInteractions(projectMapper);
    }

    @Test
    void getById_existingId_returnsMappedResponse() {
        // Arrange
        Project project = new Project();
        project.setId(7);
        ProjectResponse expected = new ProjectResponse(7, "Found", null, "en", "uk", ProjectStatus.DRAFT);
        when(projectRepository.findById(7)).thenReturn(Optional.of(project));
        when(projectMapper.toResponse(project)).thenReturn(expected);

        // Act
        ProjectResponse result = projectService.getById(7);

        // Assert
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getById_unknownId_throwsIllegalArgumentExceptionNamingTheMissingId() {
        // Arrange
        when(projectRepository.findById(404)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> projectService.getById(404))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("404");
        verifyNoInteractions(projectMapper);
    }

    @Test
    void update_existingProject_mutatesSameEntityInstanceAndPersistsIt() {
        // Arrange
        Project existing = new Project();
        existing.setId(3);
        existing.setName("Old name");
        ProjectUpdateRequest request = new ProjectUpdateRequest("New name", "desc", "en", "pl", ProjectStatus.ACTIVE);
        ProjectResponse expected = new ProjectResponse(3, "New name", "desc", "en", "pl", ProjectStatus.ACTIVE);

        when(projectRepository.findById(3)).thenReturn(Optional.of(existing));
        when(projectRepository.save(existing)).thenReturn(existing);
        when(projectMapper.toResponse(existing)).thenReturn(expected);

        // Act
        ProjectResponse result = projectService.update(3, request);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(projectMapper).updateEntity(request, existing); // in-place mutation, not a new object
        verify(projectRepository).save(existing);
    }

    @Test
    void update_unknownId_throwsIllegalArgumentException_andNeverTouchesSaveOrMapper() {
        // Arrange
        ProjectUpdateRequest request = new ProjectUpdateRequest("X", null, "en", "uk", ProjectStatus.DRAFT);
        when(projectRepository.findById(99)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> projectService.update(99, request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(projectRepository, never()).save(any());
        verify(projectMapper, never()).updateEntity(any(), any());
    }

    @Test
    void delete_existingProject_removesTheFetchedEntity() {
        // Arrange
        Project existing = new Project();
        existing.setId(5);
        when(projectRepository.findById(5)).thenReturn(Optional.of(existing));

        // Act
        projectService.delete(5);

        // Assert
        ArgumentCaptor<Project> deletedCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).delete(deletedCaptor.capture());
        assertThat(deletedCaptor.getValue().getId()).isEqualTo(5);
    }

    @Test
    void delete_unknownId_throwsIllegalArgumentException_andNeverCallsDelete() {
        // Arrange
        when(projectRepository.findById(123)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> projectService.delete(123))
                .isInstanceOf(IllegalArgumentException.class);
        verify(projectRepository, never()).delete(any());
    }
}

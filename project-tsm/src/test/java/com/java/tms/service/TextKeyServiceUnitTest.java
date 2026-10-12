package com.java.tms.service;

import com.java.tms.domain.dto.TextKeyCreateRequest;
import com.java.tms.domain.dto.TextKeyResponse;
import com.java.tms.domain.mapper.TextKeyMapper;
import com.java.tms.domain.model.Project;
import com.java.tms.domain.model.TextKey;
import com.java.tms.repository.ProjectRepository;
import com.java.tms.repository.TextKeyRepository;
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
 * Pure Mockito unit tests for {@link TextKeyService} — Lab 2 (Unit Testing).
 * No Spring context, no real database (AC3/AC5); every public method has a happy-path
 * test (AC1) and at least one invalid/edge-case test (AC2) — AC8 for this class.
 */
@ExtendWith(MockitoExtension.class)
class TextKeyServiceUnitTest {

    @Mock
    private TextKeyRepository textKeyRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private TextKeyMapper textKeyMapper;
    @InjectMocks
    private TextKeyService textKeyService;

    @Test
    void create_existingProject_linksNewTextKeyToThatProjectBeforeSaving() {
        // Arrange
        Project project = new Project();
        project.setId(1);
        TextKeyCreateRequest request = new TextKeyCreateRequest("charon.greet", "Hello", null);
        TextKey entityFromMapper = new TextKey(); // no project set yet — service must set it
        TextKey saved = new TextKey();
        saved.setId(10);
        saved.setProject(project);
        TextKeyResponse expected = new TextKeyResponse(10, null, "charon.greet", "Hello", null);

        when(projectRepository.findById(1)).thenReturn(Optional.of(project));
        when(textKeyMapper.toEntity(request)).thenReturn(entityFromMapper);
        when(textKeyRepository.save(entityFromMapper)).thenReturn(saved);
        when(textKeyMapper.toResponse(saved)).thenReturn(expected);

        // Act
        TextKeyResponse result = textKeyService.create(1, request);

        // Assert
        assertThat(result).isEqualTo(expected);
        assertThat(entityFromMapper.getProject()).isSameAs(project); // the key fact this method exists to guarantee
    }

    @Test
    void create_unknownProject_throwsIllegalArgumentException_andNeverSaves() {
        // Arrange
        TextKeyCreateRequest request = new TextKeyCreateRequest("k", "v", null);
        when(projectRepository.findById(404)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> textKeyService.create(404, request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(textKeyRepository, never()).save(any());
    }

    @Test
    void listByProject_withSeveralKeys_assignsSequentialDisplayNumbersIndependentOfRealId() {
        // Arrange: ids are deliberately out of a 1..n range to prove "number" != "id"
        when(projectRepository.existsById(1)).thenReturn(true);
        TextKey keyA = new TextKey();
        keyA.setId(47);
        TextKey keyB = new TextKey();
        keyB.setId(52);
        when(textKeyRepository.findByProjectIdOrderByIdAsc(1)).thenReturn(List.of(keyA, keyB));
        when(textKeyMapper.toResponse(keyA)).thenReturn(new TextKeyResponse(47, null, "a", "A", null));
        when(textKeyMapper.toResponse(keyB)).thenReturn(new TextKeyResponse(52, null, "b", "B", null));

        // Act
        List<TextKeyResponse> result = textKeyService.listByProject(1);

        // Assert
        assertThat(result).extracting(TextKeyResponse::getNumber).containsExactly(1, 2);
        assertThat(result).extracting(TextKeyResponse::getId).containsExactly(47, 52);
    }

    @Test
    void listByProject_unknownProject_throwsIllegalArgumentException_andNeverQueriesKeys() {
        // Arrange
        when(projectRepository.existsById(999)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> textKeyService.listByProject(999))
                .isInstanceOf(IllegalArgumentException.class);
        verify(textKeyRepository, never()).findByProjectIdOrderByIdAsc(any());
    }

    @Test
    void update_keyBelongsToGivenProject_updatesInPlaceAndPersists() {
        // Arrange
        Project project = new Project();
        project.setId(1);
        TextKey existing = new TextKey();
        existing.setId(10);
        existing.setProject(project);
        TextKeyCreateRequest request = new TextKeyCreateRequest("k2", "updated text", "note");
        TextKeyResponse expected = new TextKeyResponse(10, null, "k2", "updated text", "note");

        when(textKeyRepository.findById(10)).thenReturn(Optional.of(existing));
        when(textKeyRepository.save(existing)).thenReturn(existing);
        when(textKeyMapper.toResponse(existing)).thenReturn(expected);

        // Act
        TextKeyResponse result = textKeyService.update(1, 10, request);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(textKeyMapper).updateEntity(request, existing);
    }

    @Test
    void update_unknownId_throwsIllegalArgumentException() {
        // Arrange
        when(textKeyRepository.findById(404)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> textKeyService.update(1, 404, new TextKeyCreateRequest("k", "v", null)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(textKeyRepository, never()).save(any());
    }

    @Test
    void update_keyBelongsToDifferentProject_throwsIllegalArgumentException_andNeverSaves() {
        // Arrange: text key 10 actually belongs to project 1, caller claims project 2
        Project actualProject = new Project();
        actualProject.setId(1);
        TextKey existing = new TextKey();
        existing.setId(10);
        existing.setProject(actualProject);
        when(textKeyRepository.findById(10)).thenReturn(Optional.of(existing));

        // Act + Assert
        assertThatThrownBy(() -> textKeyService.update(2, 10, new TextKeyCreateRequest("k", "v", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");
        verify(textKeyRepository, never()).save(any());
    }

    @Test
    void delete_keyBelongsToGivenProject_removesTheFetchedEntity() {
        // Arrange
        Project project = new Project();
        project.setId(1);
        TextKey existing = new TextKey();
        existing.setId(10);
        existing.setProject(project);
        when(textKeyRepository.findById(10)).thenReturn(Optional.of(existing));

        // Act
        textKeyService.delete(1, 10);

        // Assert
        ArgumentCaptor<TextKey> captor = ArgumentCaptor.forClass(TextKey.class);
        verify(textKeyRepository).delete(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(10);
    }

    @Test
    void delete_keyBelongsToDifferentProject_throwsIllegalArgumentException_andNeverDeletes() {
        // Arrange
        Project actualProject = new Project();
        actualProject.setId(1);
        TextKey existing = new TextKey();
        existing.setId(10);
        existing.setProject(actualProject);
        when(textKeyRepository.findById(10)).thenReturn(Optional.of(existing));

        // Act + Assert
        assertThatThrownBy(() -> textKeyService.delete(2, 10))
                .isInstanceOf(IllegalArgumentException.class);
        verify(textKeyRepository, never()).delete(any());
    }
}

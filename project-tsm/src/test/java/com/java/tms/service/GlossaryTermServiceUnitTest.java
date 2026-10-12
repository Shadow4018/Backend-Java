package com.java.tms.service;

import com.java.tms.domain.dto.GlossaryTermCreateRequest;
import com.java.tms.domain.dto.GlossaryTermResponse;
import com.java.tms.domain.mapper.GlossaryTermMapper;
import com.java.tms.domain.model.GlossaryTerm;
import com.java.tms.repository.GlossaryTermRepository;
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
 * Pure Mockito unit tests for {@link GlossaryTermService} — Lab 2 (Unit Testing).
 * No Spring context, no real database (AC3/AC5); every public method has a happy-path
 * test (AC1) and at least one invalid/edge-case test (AC2) — AC8 for this class.
 */
@ExtendWith(MockitoExtension.class)
class GlossaryTermServiceUnitTest {

    @Mock
    private GlossaryTermRepository glossaryTermRepository;
    @Mock
    private GlossaryTermMapper glossaryTermMapper;
    @InjectMocks
    private GlossaryTermService glossaryTermService;

    @Test
    void create_validRequest_savesEntityBuiltFromMapperAndReturnsMappedResponse() {
        // Arrange
        GlossaryTermCreateRequest request = new GlossaryTermCreateRequest("Sinner", "Грішник", "main characters");
        GlossaryTerm fromMapper = new GlossaryTerm();
        GlossaryTerm saved = new GlossaryTerm();
        saved.setId(1);
        GlossaryTermResponse expected = new GlossaryTermResponse(1, "Sinner", "Грішник", "main characters");

        when(glossaryTermMapper.toEntity(request)).thenReturn(fromMapper);
        when(glossaryTermRepository.save(fromMapper)).thenReturn(saved);
        when(glossaryTermMapper.toResponse(saved)).thenReturn(expected);

        // Act
        GlossaryTermResponse result = glossaryTermService.create(request);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(glossaryTermRepository).save(fromMapper);
    }

    @Test
    void create_repositoryRejectsDuplicateTerm_exceptionPropagatesUnwrapped() {
        // Arrange: "term" has a unique constraint in the DB
        GlossaryTermCreateRequest request = new GlossaryTermCreateRequest("Sinner", "Грішник", null);
        GlossaryTerm fromMapper = new GlossaryTerm();
        when(glossaryTermMapper.toEntity(request)).thenReturn(fromMapper);
        when(glossaryTermRepository.save(fromMapper))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate term"));

        // Act + Assert
        assertThatThrownBy(() -> glossaryTermService.create(request))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        verify(glossaryTermMapper, never()).toResponse(any());
    }

    @Test
    void listAll_withTerms_returnsResponsesInRepositoryOrder() {
        // Arrange
        GlossaryTerm termA = new GlossaryTerm();
        termA.setId(1);
        GlossaryTerm termB = new GlossaryTerm();
        termB.setId(2);
        GlossaryTermResponse responseA = new GlossaryTermResponse(1, "A", "А", null);
        GlossaryTermResponse responseB = new GlossaryTermResponse(2, "B", "Б", null);
        when(glossaryTermRepository.findAll()).thenReturn(List.of(termA, termB));
        when(glossaryTermMapper.toResponse(termA)).thenReturn(responseA);
        when(glossaryTermMapper.toResponse(termB)).thenReturn(responseB);

        // Act
        List<GlossaryTermResponse> result = glossaryTermService.listAll();

        // Assert
        assertThat(result).containsExactly(responseA, responseB);
    }

    @Test
    void listAll_whenGlossaryIsEmpty_returnsEmptyListWithoutMapperCalls() {
        // Arrange
        when(glossaryTermRepository.findAll()).thenReturn(List.of());

        // Act
        List<GlossaryTermResponse> result = glossaryTermService.listAll();

        // Assert
        assertThat(result).isEmpty();
        verifyNoInteractions(glossaryTermMapper);
    }

    @Test
    void update_existingTerm_mutatesSameEntityInstanceAndPersistsIt() {
        // Arrange
        GlossaryTerm existing = new GlossaryTerm();
        existing.setId(1);
        existing.setTerm("Sinner");
        GlossaryTermCreateRequest request = new GlossaryTermCreateRequest("Sinner", "Грішник (new)", "updated");
        GlossaryTermResponse expected = new GlossaryTermResponse(1, "Sinner", "Грішник (new)", "updated");

        when(glossaryTermRepository.findById(1)).thenReturn(Optional.of(existing));
        when(glossaryTermRepository.save(existing)).thenReturn(existing);
        when(glossaryTermMapper.toResponse(existing)).thenReturn(expected);

        // Act
        GlossaryTermResponse result = glossaryTermService.update(1, request);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(glossaryTermMapper).updateEntity(request, existing);
    }

    @Test
    void update_unknownId_throwsIllegalArgumentException_andNeverSaves() {
        // Arrange
        when(glossaryTermRepository.findById(404)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> glossaryTermService.update(404, new GlossaryTermCreateRequest("x", "y", null)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(glossaryTermRepository, never()).save(any());
    }

    @Test
    void delete_existingTerm_removesTheFetchedEntity() {
        // Arrange
        GlossaryTerm existing = new GlossaryTerm();
        existing.setId(1);
        when(glossaryTermRepository.findById(1)).thenReturn(Optional.of(existing));

        // Act
        glossaryTermService.delete(1);

        // Assert
        ArgumentCaptor<GlossaryTerm> captor = ArgumentCaptor.forClass(GlossaryTerm.class);
        verify(glossaryTermRepository).delete(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1);
    }

    @Test
    void delete_unknownId_throwsIllegalArgumentException_andNeverDeletes() {
        // Arrange
        when(glossaryTermRepository.findById(404)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> glossaryTermService.delete(404))
                .isInstanceOf(IllegalArgumentException.class);
        verify(glossaryTermRepository, never()).delete(any());
    }
}

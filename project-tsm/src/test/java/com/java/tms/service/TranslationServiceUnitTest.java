package com.java.tms.service;

import com.java.tms.domain.dto.TranslationCreateRequest;
import com.java.tms.domain.dto.TranslationResponse;
import com.java.tms.domain.enums.TranslationStatus;
import com.java.tms.domain.mapper.TranslationMapper;
import com.java.tms.domain.model.TextKey;
import com.java.tms.domain.model.Translation;
import com.java.tms.repository.TextKeyRepository;
import com.java.tms.repository.TranslationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pure Mockito unit tests for {@link TranslationService} — Lab 2 (Unit Testing).
 *
 * <p>No Spring context, no real database (AC3/AC5). Note: {@code approve()} is annotated
 * {@code @PreAuthorize} in production, but that check is enforced by a Spring AOP proxy —
 * calling the method directly in a unit test bypasses it entirely, same as any plain Java
 * call would. The role-based 403 behaviour is already covered separately by
 * {@code TranslationApprovalSecurityTest} (a Spring-context integration test); here we only
 * test what the method body itself does once it is allowed to run.
 */
@ExtendWith(MockitoExtension.class)
class TranslationServiceUnitTest {

    @Mock
    private TranslationRepository translationRepository;
    @Mock
    private TextKeyRepository textKeyRepository;
    @Mock
    private TranslationMapper translationMapper;
    @InjectMocks
    private TranslationService translationService;

    // ---------- propose ----------

    @Test
    void propose_existingTextKey_savesWithServerAssignedTranslatorAndPendingStatus() {
        // Arrange
        TextKey textKey = new TextKey();
        textKey.setId(5);
        TranslationCreateRequest request = new TranslationCreateRequest("uk", "Привіт", null);
        Translation fromMapper = new Translation(); // mapper only fills languageCode/translatedText/comment
        when(textKeyRepository.findById(5)).thenReturn(Optional.of(textKey));
        when(translationMapper.toEntity(request)).thenReturn(fromMapper);
        when(translationRepository.save(fromMapper)).thenReturn(fromMapper);
        when(translationMapper.toResponse(fromMapper)).thenReturn(new TranslationResponse());

        // Act
        translationService.propose(5, request, "translator-alice");

        // Assert — the fields the SERVICE itself is responsible for, not the mapper
        ArgumentCaptor<Translation> captor = ArgumentCaptor.forClass(Translation.class);
        verify(translationRepository).save(captor.capture());
        Translation saved = captor.getValue();
        assertThat(saved.getTextKey()).isSameAs(textKey);
        assertThat(saved.getTranslatorName()).isEqualTo("translator-alice");
        assertThat(saved.getStatus()).isEqualTo(TranslationStatus.PENDING);
    }

    @Test
    void propose_unknownTextKey_throwsIllegalArgumentException_andNeverSaves() {
        // Arrange
        when(textKeyRepository.findById(404)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> translationService.propose(404, new TranslationCreateRequest("uk", "x", null), "bob"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(translationRepository, never()).save(any());
    }

    // ---------- update ----------

    @Test
    void update_ownerEditsOwnPendingTranslation_overwritesEditableFieldsAndSaves() {
        // Arrange
        Translation existing = new Translation();
        existing.setId(1);
        existing.setTranslatorName("alice");
        existing.setStatus(TranslationStatus.PENDING);
        existing.setTranslatedText("old text");
        TranslationCreateRequest request = new TranslationCreateRequest("pl", "nowy tekst", "fixed typo");
        when(translationRepository.findById(1)).thenReturn(Optional.of(existing));
        when(translationRepository.save(existing)).thenReturn(existing);
        when(translationMapper.toResponse(existing)).thenReturn(new TranslationResponse());

        // Act
        translationService.update(1, request, "alice", false);

        // Assert
        assertThat(existing.getLanguageCode()).isEqualTo("pl");
        assertThat(existing.getTranslatedText()).isEqualTo("nowy tekst");
        assertThat(existing.getComment()).isEqualTo("fixed typo");
        verify(translationRepository).save(existing);
    }

    @Test
    void update_adminEditsSomeoneElsesTranslation_bypassesOwnershipCheckAndSaves() {
        // Arrange: admin flag true, author is "alice", caller is "admin-bob"
        Translation existing = new Translation();
        existing.setId(2);
        existing.setTranslatorName("alice");
        existing.setStatus(TranslationStatus.PENDING);
        when(translationRepository.findById(2)).thenReturn(Optional.of(existing));
        when(translationRepository.save(existing)).thenReturn(existing);
        when(translationMapper.toResponse(existing)).thenReturn(new TranslationResponse());

        // Act + Assert (no exception)
        translationService.update(2, new TranslationCreateRequest("uk", "admin edit", null), "admin-bob", true);
        verify(translationRepository).save(existing);
    }

    @Test
    void update_nonOwnerNonAdmin_throwsAccessDeniedException_andNeverSaves() {
        // Arrange
        Translation existing = new Translation();
        existing.setId(3);
        existing.setTranslatorName("alice");
        existing.setStatus(TranslationStatus.PENDING);
        when(translationRepository.findById(3)).thenReturn(Optional.of(existing));

        // Act + Assert
        assertThatThrownBy(() ->
                translationService.update(3, new TranslationCreateRequest("uk", "x", null), "mallory", false))
                .isInstanceOf(AccessDeniedException.class);
        verify(translationRepository, never()).save(any());
    }

    @Test
    void update_alreadyApprovedTranslation_throwsIllegalArgumentException_evenForTheOwner() {
        // Arrange: owner tries to edit, but the translation was already approved
        Translation existing = new Translation();
        existing.setId(4);
        existing.setTranslatorName("alice");
        existing.setStatus(TranslationStatus.APPROVED);
        when(translationRepository.findById(4)).thenReturn(Optional.of(existing));

        // Act + Assert
        assertThatThrownBy(() ->
                translationService.update(4, new TranslationCreateRequest("uk", "x", null), "alice", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already approved");
        verify(translationRepository, never()).save(any());
    }

    // ---------- approve ----------

    @Test
    void approve_existingTranslation_setsStatusApprovedAndPersists() {
        // Arrange
        Translation existing = new Translation();
        existing.setId(1);
        existing.setStatus(TranslationStatus.PENDING);
        when(translationRepository.findById(1)).thenReturn(Optional.of(existing));
        when(translationRepository.save(existing)).thenReturn(existing);
        when(translationMapper.toResponse(existing)).thenReturn(new TranslationResponse());

        // Act
        translationService.approve(1);

        // Assert
        assertThat(existing.getStatus()).isEqualTo(TranslationStatus.APPROVED);
        verify(translationRepository).save(existing);
    }

    @Test
    void approve_unknownTranslation_throwsIllegalArgumentException_andNeverSaves() {
        // Arrange
        when(translationRepository.findById(404)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> translationService.approve(404))
                .isInstanceOf(IllegalArgumentException.class);
        verify(translationRepository, never()).save(any());
    }

    // ---------- delete ----------

    @Test
    void delete_ownerDeletesOwnProposal_removesTheFetchedEntity() {
        // Arrange
        Translation existing = new Translation();
        existing.setId(1);
        existing.setTranslatorName("alice");
        when(translationRepository.findById(1)).thenReturn(Optional.of(existing));

        // Act
        translationService.delete(1, "alice", false);

        // Assert
        ArgumentCaptor<Translation> captor = ArgumentCaptor.forClass(Translation.class);
        verify(translationRepository).delete(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1);
    }

    @Test
    void delete_nonOwnerNonAdmin_throwsAccessDeniedException_andNeverDeletes() {
        // Arrange
        Translation existing = new Translation();
        existing.setId(2);
        existing.setTranslatorName("alice");
        when(translationRepository.findById(2)).thenReturn(Optional.of(existing));

        // Act + Assert
        assertThatThrownBy(() -> translationService.delete(2, "mallory", false))
                .isInstanceOf(AccessDeniedException.class);
        verify(translationRepository, never()).delete(any());
    }

    // ---------- listByTextKey ----------

    @Test
    void listByTextKey_withSeveralTranslations_assignsSequentialDisplayNumbersIndependentOfRealId() {
        // Arrange: ids deliberately out of a 1..n range to prove "number" != "id"
        when(textKeyRepository.existsById(5)).thenReturn(true);
        Translation t1 = new Translation();
        t1.setId(30);
        Translation t2 = new Translation();
        t2.setId(31);
        when(translationRepository.findByTextKeyIdOrderByIdAsc(5)).thenReturn(List.of(t1, t2));
        when(translationMapper.toResponse(t1)).thenReturn(new TranslationResponse(30, null, "uk", "a", "x", TranslationStatus.PENDING, null));
        when(translationMapper.toResponse(t2)).thenReturn(new TranslationResponse(31, null, "pl", "b", "y", TranslationStatus.PENDING, null));

        // Act
        List<TranslationResponse> result = translationService.listByTextKey(5);

        // Assert
        assertThat(result).extracting(TranslationResponse::getNumber).containsExactly(1, 2);
        assertThat(result).extracting(TranslationResponse::getId).containsExactly(30, 31);
    }

    @Test
    void listByTextKey_unknownTextKey_throwsIllegalArgumentException_andNeverQueriesTranslations() {
        // Arrange
        when(textKeyRepository.existsById(999)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> translationService.listByTextKey(999))
                .isInstanceOf(IllegalArgumentException.class);
        verify(translationRepository, never()).findByTextKeyIdOrderByIdAsc(any());
    }
}

package com.java.tms.service;

import com.java.tms.domain.dto.TranslationCreateRequest;
import com.java.tms.domain.dto.TranslationResponse;
import com.java.tms.domain.enums.TranslationStatus;
import com.java.tms.domain.mapper.TranslationMapper;
import com.java.tms.domain.model.TextKey;
import com.java.tms.domain.model.Translation;
import com.java.tms.repository.TextKeyRepository;
import com.java.tms.repository.TranslationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TranslationService {

    private final TranslationRepository translationRepository;
    private final TextKeyRepository textKeyRepository;
    private final TranslationMapper translationMapper;

    @Transactional
    public TranslationResponse propose(Integer textKeyId, TranslationCreateRequest request, String translatorName) {
        TextKey textKey = textKeyRepository.findById(textKeyId)
                .orElseThrow(() -> new IllegalArgumentException("TextKey not found: " + textKeyId));

        Translation translation = translationMapper.toEntity(request);
        translation.setTextKey(textKey);
        translation.setTranslatorName(translatorName);
        translation.setStatus(TranslationStatus.PENDING);

        return translationMapper.toResponse(translationRepository.save(translation));
    }

    /**
     * A translator may correct their own pending proposal instead of deleting and
     * re-typing it. Editing an already-approved translation is not allowed — it would
     * silently invalidate a decision an editor already made; delete it and propose a
     * new one instead.
     */
    @Transactional
    public TranslationResponse update(Integer translationId, TranslationCreateRequest request,
                                       String currentUsername, boolean isAdmin) {
        Translation translation = findOrThrow(translationId);
        requireOwnerOrAdmin(translation, currentUsername, isAdmin, "edit");

        if (translation.getStatus() == TranslationStatus.APPROVED) {
            throw new IllegalArgumentException(
                    "Translation " + translationId + " is already approved and can no longer be edited");
        }

        translation.setLanguageCode(request.getLanguageCode());
        translation.setTranslatedText(request.getTranslatedText());
        translation.setComment(request.getComment());

        return translationMapper.toResponse(translationRepository.save(translation));
    }

    /**
     * Approving a translation is restricted to ADMIN/EDITOR here, via method security,
     * independently of any {@code SecurityFilterChain} rule (AC7).
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @Transactional
    public TranslationResponse approve(Integer translationId) {
        Translation translation = findOrThrow(translationId);
        translation.setStatus(TranslationStatus.APPROVED);
        return translationMapper.toResponse(translationRepository.save(translation));
    }

    /**
     * ADMIN/EDITOR may delete any translation; a TRANSLATOR may delete only their own
     * proposal (e.g. after a mistake), checked by comparing {@code translatorName} to the
     * caller's username — not by role alone.
     */
    @Transactional
    public void delete(Integer translationId, String currentUsername, boolean isAdmin) {
        Translation translation = findOrThrow(translationId);
        requireOwnerOrAdmin(translation, currentUsername, isAdmin, "delete");
        translationRepository.delete(translation);
    }

    @Transactional(readOnly = true)
    public List<TranslationResponse> listByTextKey(Integer textKeyId) {
        if (!textKeyRepository.existsById(textKeyId)) {
            throw new IllegalArgumentException("TextKey not found: " + textKeyId);
        }
        List<Translation> translations = translationRepository.findByTextKeyIdOrderByIdAsc(textKeyId);
        List<TranslationResponse> responses = new ArrayList<>(translations.size());
        // Same display-only numbering as TextKeyService#listByProject: "number" restarts
        // at 1 for each text key; the real "id" keeps its own, never-reused sequence.
        for (int i = 0; i < translations.size(); i++) {
            TranslationResponse response = translationMapper.toResponse(translations.get(i));
            response.setNumber(i + 1);
            responses.add(response);
        }
        return responses;
    }

    private void requireOwnerOrAdmin(Translation translation, String currentUsername, boolean isAdmin, String action) {
        boolean isOwner = translation.getTranslatorName() != null
                && translation.getTranslatorName().equals(currentUsername);
        // Editors approve; they don't otherwise own or manage translator proposals, so an
        // editor who is not also an admin still can't edit/delete someone else's proposal.
        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException(
                    "Only the author of this translation proposal (or an admin) may " + action + " it");
        }
    }

    private Translation findOrThrow(Integer translationId) {
        return translationRepository.findById(translationId)
                .orElseThrow(() -> new IllegalArgumentException("Translation not found: " + translationId));
    }
}

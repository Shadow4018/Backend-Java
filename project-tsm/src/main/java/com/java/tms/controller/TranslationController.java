package com.java.tms.controller;

import com.java.tms.domain.dto.TranslationCreateRequest;
import com.java.tms.domain.dto.TranslationResponse;
import com.java.tms.service.TranslationService;
import com.java.tms.web.annotation.CurrentUsername;
import com.java.tms.web.annotation.PostMappingCreated;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TranslationController {

    private final TranslationService translationService;

    @GetMapping("/text-keys/{textKeyId}/translations")
    public List<TranslationResponse> listByTextKey(@PathVariable Integer textKeyId) {
        return translationService.listByTextKey(textKeyId);
    }

    @PostMappingCreated("/text-keys/{textKeyId}/translations")
    public TranslationResponse propose(@PathVariable Integer textKeyId,
                                        @Valid @RequestBody TranslationCreateRequest request,
                                        @CurrentUsername String translatorName) {
        return translationService.propose(textKeyId, request, translatorName);
    }

    /**
     * Lets a translator fix their own pending proposal in place. Ownership (or admin
     * rights) is checked in {@link TranslationService#update}, not here.
     */
    @PutMapping("/text-keys/{textKeyId}/translations/{translationId}")
    public TranslationResponse update(@PathVariable Integer textKeyId, @PathVariable Integer translationId,
                                       @Valid @RequestBody TranslationCreateRequest request,
                                       @CurrentUsername String currentUsername,
                                       Authentication authentication) {
        return translationService.update(translationId, request, currentUsername, isAdmin(authentication));
    }

    @PatchMapping("/translations/{translationId}/approve")
    public TranslationResponse approve(@PathVariable Integer translationId) {
        return translationService.approve(translationId);
    }

    @DeleteMapping("/translations/{translationId}")
    public ResponseEntity<Void> delete(@PathVariable Integer translationId,
                                        @CurrentUsername String currentUsername,
                                        Authentication authentication) {
        translationService.delete(translationId, currentUsername, isAdmin(authentication));
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}

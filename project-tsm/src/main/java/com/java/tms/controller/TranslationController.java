package com.java.tms.controller;

import com.java.tms.domain.dto.TranslationCreateRequest;
import com.java.tms.domain.dto.TranslationResponse;
import com.java.tms.service.TranslationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
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

    @PostMapping("/text-keys/{textKeyId}/translations")
    @ResponseStatus(HttpStatus.CREATED)
    public TranslationResponse propose(@PathVariable Integer textKeyId,
                                        @Valid @RequestBody TranslationCreateRequest request,
                                        Authentication authentication) {
        return translationService.propose(textKeyId, request, usernameOf(authentication));
    }

    /**
     * Lets a translator fix their own pending proposal in place. Ownership (or admin
     * rights) is checked in {@link TranslationService#update}, not here.
     */
    @PutMapping("/text-keys/{textKeyId}/translations/{translationId}")
    public TranslationResponse update(@PathVariable Integer textKeyId, @PathVariable Integer translationId,
                                       @Valid @RequestBody TranslationCreateRequest request,
                                       Authentication authentication) {
        return translationService.update(translationId, request, usernameOf(authentication), isAdmin(authentication));
    }

    @PatchMapping("/translations/{translationId}/approve")
    public TranslationResponse approve(@PathVariable Integer translationId) {
        return translationService.approve(translationId);
    }

    @DeleteMapping("/translations/{translationId}")
    public ResponseEntity<Void> delete(@PathVariable Integer translationId, Authentication authentication) {
        translationService.delete(translationId, usernameOf(authentication), isAdmin(authentication));
        return ResponseEntity.noContent().build();
    }

    private String usernameOf(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("preferred_username");
        }
        return authentication.getName();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}

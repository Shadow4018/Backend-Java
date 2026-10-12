package com.java.tms.controller;

import com.java.tms.domain.dto.TextKeyCreateRequest;
import com.java.tms.domain.dto.TextKeyResponse;
import com.java.tms.service.TextKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/text-keys")
@RequiredArgsConstructor
public class TextKeyController {

    private final TextKeyService textKeyService;

    @GetMapping
    public List<TextKeyResponse> listByProject(@PathVariable Integer projectId) {
        return textKeyService.listByProject(projectId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TextKeyResponse create(@PathVariable Integer projectId,
                                   @Valid @RequestBody TextKeyCreateRequest request) {
        return textKeyService.create(projectId, request);
    }

    @PutMapping("/{id}")
    public TextKeyResponse update(@PathVariable Integer projectId, @PathVariable Integer id,
                                   @Valid @RequestBody TextKeyCreateRequest request) {
        return textKeyService.update(projectId, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer projectId, @PathVariable Integer id) {
        textKeyService.delete(projectId, id);
        return ResponseEntity.noContent().build();
    }
}

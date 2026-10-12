package com.java.tms.controller;

import com.java.tms.domain.dto.GlossaryTermCreateRequest;
import com.java.tms.domain.dto.GlossaryTermResponse;
import com.java.tms.service.GlossaryTermService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/glossary")
@RequiredArgsConstructor
public class GlossaryTermController {

    private final GlossaryTermService glossaryTermService;

    @GetMapping
    public List<GlossaryTermResponse> listAll() {
        return glossaryTermService.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GlossaryTermResponse create(@Valid @RequestBody GlossaryTermCreateRequest request) {
        return glossaryTermService.create(request);
    }

    @PutMapping("/{id}")
    public GlossaryTermResponse update(@PathVariable Integer id, @Valid @RequestBody GlossaryTermCreateRequest request) {
        return glossaryTermService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        glossaryTermService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

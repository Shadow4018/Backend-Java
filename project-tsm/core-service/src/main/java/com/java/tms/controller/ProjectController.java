package com.java.tms.controller;

import com.java.tms.async.ProjectExportService;
import com.java.tms.domain.dto.ProjectCreateRequest;
import com.java.tms.domain.dto.ProjectExportSummary;
import com.java.tms.domain.dto.ProjectResponse;
import com.java.tms.domain.dto.ProjectUpdateRequest;
import com.java.tms.service.ProjectService;
import com.java.tms.web.annotation.PostMappingCreated;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for project management (full CUD).
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectExportService projectExportService;

    @GetMapping
    public List<ProjectResponse> listAll() {
        return projectService.listAll();
    }

    /**
     * Lab 5: combines two independent ~1s slow calls (PDF generation + an external
     * glossary-suggestions lookup) concurrently via {@link ProjectExportService}. Either call
     * failing, or the combined wait exceeding its bound, is reported in the response body
     * ({@link ProjectExportSummary#errorMessage()}) - never as an unexpected 500.
     */
    @GetMapping("/{id}/export-summary")
    public ProjectExportSummary exportSummary(@PathVariable Integer id) {
        return projectExportService.buildExportSummary(id);
    }

    @GetMapping("/{id}")
    public ProjectResponse getById(@PathVariable Integer id) {
        return projectService.getById(id);
    }

    @PostMappingCreated
    public ProjectResponse create(@Valid @RequestBody ProjectCreateRequest request) {
        return projectService.create(request);
    }

    @PutMapping("/{id}")
    public ProjectResponse update(@PathVariable Integer id, @Valid @RequestBody ProjectUpdateRequest request) {
        return projectService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

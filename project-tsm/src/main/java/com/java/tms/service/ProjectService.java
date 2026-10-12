package com.java.tms.service;

import com.java.tms.domain.dto.ProjectCreateRequest;
import com.java.tms.domain.dto.ProjectResponse;
import com.java.tms.domain.dto.ProjectUpdateRequest;
import com.java.tms.domain.mapper.ProjectMapper;
import com.java.tms.domain.model.Project;
import com.java.tms.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for creating, reading, updating and deleting translation projects.
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    @Transactional
    public ProjectResponse create(ProjectCreateRequest request) {
        Project project = projectMapper.toEntity(request);
        return projectMapper.toResponse(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listAll() {
        return projectRepository.findAll().stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getById(Integer id) {
        return projectMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public ProjectResponse update(Integer id, ProjectUpdateRequest request) {
        Project project = findOrThrow(id);
        projectMapper.updateEntity(request, project);
        return projectMapper.toResponse(projectRepository.save(project));
    }

    @Transactional
    public void delete(Integer id) {
        Project project = findOrThrow(id);
        projectRepository.delete(project);
    }

    private Project findOrThrow(Integer id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));
    }
}

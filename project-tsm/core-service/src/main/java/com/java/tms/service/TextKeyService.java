package com.java.tms.service;

import com.java.tms.domain.dto.TextKeyCreateRequest;
import com.java.tms.domain.dto.TextKeyResponse;
import com.java.tms.domain.mapper.TextKeyMapper;
import com.java.tms.domain.model.Project;
import com.java.tms.domain.model.TextKey;
import com.java.tms.repository.ProjectRepository;
import com.java.tms.repository.TextKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TextKeyService {

    private final TextKeyRepository textKeyRepository;
    private final ProjectRepository projectRepository;
    private final TextKeyMapper textKeyMapper;

    @Transactional
    public TextKeyResponse create(Integer projectId, TextKeyCreateRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        TextKey textKey = textKeyMapper.toEntity(request);
        textKey.setProject(project);
        return textKeyMapper.toResponse(textKeyRepository.save(textKey));
    }

    @Transactional(readOnly = true)
    public List<TextKeyResponse> listByProject(Integer projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new IllegalArgumentException("Project not found: " + projectId);
        }
        List<TextKey> keys = textKeyRepository.findByProjectIdOrderByIdAsc(projectId);
        List<TextKeyResponse> responses = new ArrayList<>(keys.size());
        // "number" is a per-project display position (1, 2, 3, ...), independent of the
        // globally auto-incrementing "id". The id is never reused/reset — that is intentional,
        // a shared primary key sequence is standard practice — this field only gives each new
        // project its own visual "1, 2, 3...", the way it would look in a spreadsheet.
        for (int i = 0; i < keys.size(); i++) {
            TextKeyResponse response = textKeyMapper.toResponse(keys.get(i));
            response.setNumber(i + 1);
            responses.add(response);
        }
        return responses;
    }

    @Transactional
    public TextKeyResponse update(Integer projectId, Integer id, TextKeyCreateRequest request) {
        TextKey textKey = findInProjectOrThrow(projectId, id);
        textKeyMapper.updateEntity(request, textKey);
        return textKeyMapper.toResponse(textKeyRepository.save(textKey));
    }

    @Transactional
    public void delete(Integer projectId, Integer id) {
        TextKey textKey = findInProjectOrThrow(projectId, id);
        textKeyRepository.delete(textKey);
    }

    private TextKey findInProjectOrThrow(Integer projectId, Integer id) {
        TextKey textKey = textKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TextKey not found: " + id));
        if (!textKey.getProject().getId().equals(projectId)) {
            throw new IllegalArgumentException("TextKey " + id + " does not belong to project " + projectId);
        }
        return textKey;
    }
}

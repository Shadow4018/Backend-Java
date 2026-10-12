package com.java.tms.service;

import com.java.tms.domain.dto.GlossaryTermCreateRequest;
import com.java.tms.domain.dto.GlossaryTermResponse;
import com.java.tms.domain.mapper.GlossaryTermMapper;
import com.java.tms.domain.model.GlossaryTerm;
import com.java.tms.repository.GlossaryTermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GlossaryTermService {

    private final GlossaryTermRepository glossaryTermRepository;
    private final GlossaryTermMapper glossaryTermMapper;

    @Transactional
    public GlossaryTermResponse create(GlossaryTermCreateRequest request) {
        GlossaryTerm term = glossaryTermMapper.toEntity(request);
        return glossaryTermMapper.toResponse(glossaryTermRepository.save(term));
    }

    @Transactional(readOnly = true)
    public List<GlossaryTermResponse> listAll() {
        return glossaryTermRepository.findAll().stream()
                .map(glossaryTermMapper::toResponse)
                .toList();
    }

    @Transactional
    public GlossaryTermResponse update(Integer id, GlossaryTermCreateRequest request) {
        GlossaryTerm term = findOrThrow(id);
        glossaryTermMapper.updateEntity(request, term);
        return glossaryTermMapper.toResponse(glossaryTermRepository.save(term));
    }

    @Transactional
    public void delete(Integer id) {
        glossaryTermRepository.delete(findOrThrow(id));
    }

    private GlossaryTerm findOrThrow(Integer id) {
        return glossaryTermRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("GlossaryTerm not found: " + id));
    }
}

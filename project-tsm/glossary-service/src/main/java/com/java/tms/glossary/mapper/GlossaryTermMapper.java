package com.java.tms.glossary.mapper;

import com.java.tms.glossary.dto.GlossaryTermCreateRequest;
import com.java.tms.glossary.dto.GlossaryTermResponse;
import com.java.tms.glossary.model.GlossaryTerm;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GlossaryTermMapper {
    GlossaryTermResponse toResponse(GlossaryTerm term);

    GlossaryTerm toEntity(GlossaryTermCreateRequest request);

    void updateEntity(GlossaryTermCreateRequest request, @MappingTarget GlossaryTerm term);
}

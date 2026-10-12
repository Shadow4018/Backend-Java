package com.java.tms.domain.mapper;

import com.java.tms.domain.dto.GlossaryTermCreateRequest;
import com.java.tms.domain.dto.GlossaryTermResponse;
import com.java.tms.domain.model.GlossaryTerm;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GlossaryTermMapper {
    GlossaryTermResponse toResponse(GlossaryTerm term);

    GlossaryTerm toEntity(GlossaryTermCreateRequest request);

    void updateEntity(GlossaryTermCreateRequest request, @MappingTarget GlossaryTerm term);
}

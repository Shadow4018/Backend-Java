package com.java.tms.domain.mapper;

import com.java.tms.domain.dto.TranslationCreateRequest;
import com.java.tms.domain.dto.TranslationResponse;
import com.java.tms.domain.model.Translation;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TranslationMapper {
    TranslationResponse toResponse(Translation translation);

    Translation toEntity(TranslationCreateRequest request);

    void updateEntity(TranslationCreateRequest request, @MappingTarget Translation translation);
}

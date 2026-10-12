package com.java.tms.domain.mapper;

import com.java.tms.domain.dto.TextKeyCreateRequest;
import com.java.tms.domain.dto.TextKeyResponse;
import com.java.tms.domain.model.TextKey;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TextKeyMapper {
    TextKeyResponse toResponse(TextKey textKey);

    TextKey toEntity(TextKeyCreateRequest request);

    void updateEntity(TextKeyCreateRequest request, @MappingTarget TextKey textKey);
}

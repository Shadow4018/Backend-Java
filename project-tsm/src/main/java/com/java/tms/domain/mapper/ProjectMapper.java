package com.java.tms.domain.mapper;

import com.java.tms.domain.dto.ProjectCreateRequest;
import com.java.tms.domain.dto.ProjectResponse;
import com.java.tms.domain.dto.ProjectUpdateRequest;
import com.java.tms.domain.model.Project;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProjectMapper {
    ProjectResponse toResponse(Project project);

    Project toEntity(ProjectCreateRequest request);

    void updateEntity(ProjectUpdateRequest request, @MappingTarget Project project);
}

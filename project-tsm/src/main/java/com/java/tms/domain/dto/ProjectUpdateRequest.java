package com.java.tms.domain.dto;

import com.java.tms.domain.enums.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectUpdateRequest {

    @NotBlank
    private String name;

    private String description;

    @NotBlank
    private String sourceLanguage;

    @NotBlank
    private String targetLanguage;

    @NotNull
    private ProjectStatus status;
}

package com.java.tms.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectCreateRequest {

    @NotBlank
    private String name;

    private String description;

    @NotBlank
    private String sourceLanguage;

    @NotBlank
    private String targetLanguage;
}

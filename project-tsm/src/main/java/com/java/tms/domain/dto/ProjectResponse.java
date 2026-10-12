package com.java.tms.domain.dto;

import com.java.tms.domain.enums.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private Integer id;
    private String name;
    private String description;
    private String sourceLanguage;
    private String targetLanguage;
    private ProjectStatus status;
}

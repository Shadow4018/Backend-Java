package com.java.tms.domain.dto;

import com.java.tms.domain.validation.DifferentLanguages;
import com.java.tms.domain.validation.LanguagePair;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@DifferentLanguages
public class ProjectCreateRequest implements LanguagePair {

    @NotBlank
    private String name;

    private String description;

    @NotBlank
    private String sourceLanguage;

    @NotBlank
    private String targetLanguage;
}

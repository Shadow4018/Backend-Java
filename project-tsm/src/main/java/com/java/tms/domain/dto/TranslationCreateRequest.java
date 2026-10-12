package com.java.tms.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Автор перекладу береться з JWT/сесії (preferred_username), а не з тіла запиту.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TranslationCreateRequest {

    @NotBlank
    private String languageCode;

    @NotBlank
    private String translatedText;

    private String comment;
}

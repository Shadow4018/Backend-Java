package com.java.tms.domain.dto;

import com.java.tms.domain.enums.TranslationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TranslationResponse {
    private Integer id;

    // 1-based position within the text key — for display only, NOT the primary key.
    private Integer number;
    private String languageCode;
    private String translatedText;
    private String translatorName;
    private TranslationStatus status;
    private String comment;
}

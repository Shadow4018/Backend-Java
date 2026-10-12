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
public class TextKeyCreateRequest {

    @NotBlank
    private String keyName;

    @NotBlank
    private String sourceText;

    private String contextNote;
}

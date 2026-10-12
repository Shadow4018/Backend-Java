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
public class GlossaryTermCreateRequest {

    @NotBlank
    private String term;

    @NotBlank
    private String translation;

    private String description;
}

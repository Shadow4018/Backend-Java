package com.java.tms.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GlossaryTermResponse {
    private Integer id;
    private String term;
    private String translation;
    private String description;
}

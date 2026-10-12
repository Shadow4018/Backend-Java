package com.java.tms.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TextKeyResponse {
    private Integer id;

    // 1-based position within the project — for display only, NOT the primary key.
    private Integer number;
    private String keyName;
    private String sourceText;
    private String contextNote;
}

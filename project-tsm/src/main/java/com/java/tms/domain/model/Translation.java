package com.java.tms.domain.model;

import com.java.tms.domain.enums.TranslationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Single translation proposal for a text key.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "translations")
public class Translation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "text_key_id", nullable = false)
    private TextKey textKey;

    @Column(nullable = false)
    private String languageCode;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String translatedText;

    @Column(nullable = false)
    private String translatorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TranslationStatus status = TranslationStatus.PENDING;

    @Column
    private String comment;
}

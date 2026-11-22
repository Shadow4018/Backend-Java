package com.java.tms.domain.validation;

/**
 * Implemented by any DTO that carries a source/target language pair, so that
 * {@link DifferentLanguagesValidator} can validate it without caring which concrete DTO
 * it is looking at (no reflection, no hardcoded field names).
 */
public interface LanguagePair {

    String getSourceLanguage();

    String getTargetLanguage();
}

package com.java.tms.repository;

import com.java.tms.domain.model.Translation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TranslationRepository extends JpaRepository<Translation, Integer> {
    List<Translation> findByTextKeyIdOrderByIdAsc(Integer textKeyId);
}

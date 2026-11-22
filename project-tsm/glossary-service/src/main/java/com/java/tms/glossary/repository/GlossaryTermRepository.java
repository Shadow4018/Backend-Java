package com.java.tms.glossary.repository;

import com.java.tms.glossary.model.GlossaryTerm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlossaryTermRepository extends JpaRepository<GlossaryTerm, Integer> {
}

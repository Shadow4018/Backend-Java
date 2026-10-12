package com.java.tms.repository;

import com.java.tms.domain.model.GlossaryTerm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlossaryTermRepository extends JpaRepository<GlossaryTerm, Integer> {
}

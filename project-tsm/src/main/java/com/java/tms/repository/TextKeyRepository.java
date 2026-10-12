package com.java.tms.repository;

import com.java.tms.domain.model.TextKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TextKeyRepository extends JpaRepository<TextKey, Integer> {
    List<TextKey> findByProjectIdOrderByIdAsc(Integer projectId);
}

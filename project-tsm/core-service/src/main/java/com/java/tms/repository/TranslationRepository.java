package com.java.tms.repository;

import com.java.tms.domain.dto.ProjectPendingCount;
import com.java.tms.domain.enums.TranslationStatus;
import com.java.tms.domain.model.Translation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TranslationRepository extends JpaRepository<Translation, Integer> {
    List<Translation> findByTextKeyIdOrderByIdAsc(Integer textKeyId);

    /** Lab 6: oldest translations with the given status, capped by {@code pageable} (batch size). */
    List<Translation> findByStatusOrderByIdAsc(TranslationStatus status, Pageable pageable);

    /** Lab 6: per-project number of translations in the given status (read-only report). */
    @Query("select new com.java.tms.domain.dto.ProjectPendingCount(p.id, p.name, count(t)) "
            + "from Translation t join t.textKey k join k.project p "
            + "where t.status = :status group by p.id, p.name order by p.id")
    List<ProjectPendingCount> countByProjectForStatus(@Param("status") TranslationStatus status);
}

package com.java.tms.service;

import com.java.tms.domain.enums.TranslationStatus;
import com.java.tms.domain.model.Translation;
import com.java.tms.repository.TranslationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lab 6: the business logic behind the scheduled "clean up rejected translations" job.
 *
 * <p>The transaction boundary is this method (not the Quartz job). It is <b>idempotent</b>:
 * it only ever deletes translations that are currently REJECTED, so running it twice in a row
 * leaves the data exactly as after the first run (the second run finds nothing and returns 0).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationCleanupService {

    private final TranslationRepository translationRepository;

    /**
     * Deletes at most {@code maxToDelete} REJECTED translations (oldest first).
     *
     * @return how many translations were deleted by this call
     */
    @Transactional
    public int purgeRejected(int maxToDelete, String runLabel) {
        if (maxToDelete <= 0) {
            throw new IllegalArgumentException("maxToDelete must be positive, got " + maxToDelete);
        }
        List<Translation> batch = translationRepository.findByStatusOrderByIdAsc(
                TranslationStatus.REJECTED, PageRequest.of(0, maxToDelete));
        translationRepository.deleteAll(batch);
        log.info("[cleanup:{}] deleted {} rejected translation(s) (limit {})", runLabel, batch.size(), maxToDelete);
        return batch.size();
    }
}

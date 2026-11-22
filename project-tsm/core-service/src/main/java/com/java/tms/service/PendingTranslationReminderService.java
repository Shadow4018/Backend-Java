package com.java.tms.service;

import com.java.tms.domain.dto.ProjectPendingCount;
import com.java.tms.domain.enums.TranslationStatus;
import com.java.tms.repository.TranslationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lab 6: the business logic behind the scheduled "pending translations reminder" job.
 *
 * <p>Read-only, therefore trivially idempotent: building the reminder twice for the same data
 * returns the same report and changes nothing. (A real deployment would hand the report to a
 * mail/notification sender; here it is logged, like the simulated email of Lab 5.)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PendingTranslationReminderService {

    private final TranslationRepository translationRepository;

    /**
     * @param minPending only projects with at least this many PENDING translations are reported
     * @return the projects that need attention, ordered by project id
     */
    @Transactional(readOnly = true)
    public List<ProjectPendingCount> buildReminder(int minPending) {
        List<ProjectPendingCount> report = translationRepository
                .countByProjectForStatus(TranslationStatus.PENDING).stream()
                .filter(row -> row.pendingCount() >= minPending)
                .toList();
        for (ProjectPendingCount row : report) {
            log.info("[reminder] Project '{}' (id {}) has {} translation(s) awaiting review",
                    row.projectName(), row.projectId(), row.pendingCount());
        }
        if (report.isEmpty()) {
            log.info("[reminder] No project has {} or more pending translations", minPending);
        }
        return report;
    }
}

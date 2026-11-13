package com.java.tms.domain.dto;

/**
 * One row of the "pending translations" reminder report (Lab 6): how many translations of a
 * project are still waiting for review. Also used directly as a JPQL constructor-expression
 * result in {@code TranslationRepository#countByProjectForStatus}.
 */
public record ProjectPendingCount(Integer projectId, String projectName, Long pendingCount) {
}

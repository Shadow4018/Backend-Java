package com.java.tms.domain.dto;

import java.util.List;

/**
 * Result of {@link com.java.tms.async.ProjectExportService#buildExportSummary}: the
 * combination of two independent slow calls (PDF report generation + an external
 * glossary-suggestions lookup). Exactly one of (successful data) or {@code errorMessage} is
 * meaningful for a given response — a failed or timed-out export never throws, it returns an
 * instance of this with {@code errorMessage} set instead (see the class javadoc on
 * {@code ProjectExportService}).
 */
public record ProjectExportSummary(String pdfReport, List<String> glossarySuggestions, String errorMessage) {

    public boolean isSuccess() {
        return errorMessage == null;
    }
}

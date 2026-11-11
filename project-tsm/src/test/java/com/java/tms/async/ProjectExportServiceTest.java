package com.java.tms.async;

import com.java.tms.domain.dto.ProjectExportSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-Spring-context tests for {@link ProjectExportService} — proves the three remaining
 * Lab 5 scenarios that only show up when both underlying {@code @Async} calls genuinely run
 * concurrently on real threads.
 */
@SpringBootTest
@ActiveProfiles("test")
class ProjectExportServiceTest {

    @Autowired
    private ProjectExportService projectExportService;

    @Test
    void twoIndependentOneSecondCallsActuallyRunConcurrentlyNotSequentially() {
        long start = System.nanoTime();

        ProjectExportSummary summary = projectExportService.buildExportSummary(1);

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // Each underlying call sleeps ~1s; run one after another that would be ~2s+, run
        // concurrently it should land close to ~1s. 1500ms leaves comfortable headroom for
        // scheduling overhead while still failing if they ran sequentially.
        assertThat(elapsedMs).isLessThan(1500);
        assertThat(summary.isSuccess()).isTrue();
        assertThat(summary.pdfReport()).contains("PDF report for project 1");
        assertThat(summary.glossarySuggestions()).isNotEmpty();
    }

    @Test
    void aFailureInEitherUnderlyingCallProducesAFallbackSummaryInsteadOfThrowing() {
        // projectId == null makes fetchExternalGlossarySuggestionsAsync fail internally.
        ProjectExportSummary summary = projectExportService.buildExportSummary(null);

        assertThat(summary.isSuccess()).isFalse();
        assertThat(summary.errorMessage()).contains("Simulated external glossary-suggestion service failure");
        assertThat(summary.glossarySuggestions()).isEmpty();
    }

    @Test
    void waitingForTheCombinedResultIsAlwaysBoundedByATimeout() {
        // The underlying calls take ~1s; a 200ms budget must not be able to wait that out.
        long start = System.nanoTime();

        ProjectExportSummary summary = projectExportService.buildExportSummary(1, Duration.ofMillis(200));

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isLessThan(600); // returned promptly, did not wait out the ~1s work
        assertThat(summary.isSuccess()).isFalse();
        assertThat(summary.errorMessage()).contains("timed out");
    }
}

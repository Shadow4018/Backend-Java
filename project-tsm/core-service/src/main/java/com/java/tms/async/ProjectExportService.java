package com.java.tms.async;

import com.java.tms.domain.dto.ProjectExportSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Orchestrates the "pair of independent slow calls, combined" part of Lab 5: starts
 * {@link SlowSideEffects#generatePdfReportAsync} and
 * {@link SlowSideEffects#fetchExternalGlossarySuggestionsAsync} without waiting on either one
 * individually, then combines them with {@link CompletableFuture#thenCombine}.
 *
 * <p>Both of the lab's remaining requirements are handled right here, at the one place a
 * human actually waits on the result:
 * <ul>
 *   <li><b>A failure in either call must not crash the caller</b> — {@code exceptionally}
 *       turns any failure of either underlying call into a {@link ProjectExportSummary} with
 *       {@code errorMessage} set, instead of an exception reaching whoever called
 *       {@link #buildExportSummary}.</li>
 *   <li><b>Waiting is always bounded</b> — the blocking {@code get(timeout, unit)} at the end
 *       guarantees this method returns within {@code timeout} no matter how long the
 *       underlying work takes, falling back to a timeout-flavoured summary rather than
 *       hanging forever.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectExportService {

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    private final SlowSideEffects slowSideEffects;

    public ProjectExportSummary buildExportSummary(Integer projectId) {
        return buildExportSummary(projectId, DEFAULT_TIMEOUT);
    }

    /**
     * @param timeout how long the caller is willing to wait for the combined result; exposed
     *                (rather than hardcoded) so tests can prove the bounded-wait behaviour
     *                quickly, with a short timeout, instead of actually waiting out a long one.
     */
    public ProjectExportSummary buildExportSummary(Integer projectId, Duration timeout) {
        // Both calls are started here, before either is awaited - this is what makes them
        // run concurrently rather than one after the other.
        CompletableFuture<String> pdfFuture = slowSideEffects.generatePdfReportAsync(projectId);
        CompletableFuture<List<String>> suggestionsFuture = slowSideEffects.fetchExternalGlossarySuggestionsAsync(projectId);

        CompletableFuture<ProjectExportSummary> combined = pdfFuture
                .thenCombine(suggestionsFuture, (pdfReport, suggestions) ->
                        new ProjectExportSummary(pdfReport, suggestions, null))
                .exceptionally(ex -> {
                    log.warn("Project export for project {} failed: {}", projectId, ex.getMessage());
                    return new ProjectExportSummary(null, List.of(), rootCauseMessage(ex));
                });

        try {
            return combined.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("Project export for project {} timed out after {}", projectId, timeout);
            return new ProjectExportSummary(null, List.of(), "Export timed out after " + timeout);
        } catch (ExecutionException e) {
            // Normally unreachable - exceptionally(...) above already turns a failure into a
            // successful (fallback) value - but kept as a second line of defence so this
            // method truly can never propagate an exception to its caller.
            return new ProjectExportSummary(null, List.of(), rootCauseMessage(e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ProjectExportSummary(null, List.of(), "Export was interrupted");
        }
    }

    private String rootCauseMessage(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}

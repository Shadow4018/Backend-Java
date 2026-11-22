package com.java.tms.scheduling.job;

import com.java.tms.service.TranslationCleanupService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Lab 6 job #1: removes REJECTED translations.
 *
 * <p><b>Thin on purpose:</b> read parameters, make exactly one Spring service call, done. The
 * business rule and the transaction boundary live in {@link TranslationCleanupService}.
 *
 * <p>{@link DisallowConcurrentExecution}: this job mutates data and has two triggers (hourly +
 * nightly) that can fire at the same moment, e.g. at 03:00. Quartz makes the second firing wait
 * until the first one has finished instead of letting two runs delete the same rows at once.
 *
 * <p><b>Failure strategy:</b> record the failure and wait for the next scheduled firing - no
 * immediate refire (the hourly trigger gives a natural, bounded retry).
 */
@DisallowConcurrentExecution
@Slf4j
public class TranslationCleanupJob implements Job {

    public static final String KEY_MAX_TO_DELETE = "maxToDelete";
    public static final String KEY_RUN_LABEL = "runLabel";

    private final TranslationCleanupService cleanupService;

    public TranslationCleanupJob(TranslationCleanupService cleanupService) {
        this.cleanupService = cleanupService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // Merged map = JobDetail defaults overridden by the firing trigger's own values.
        JobDataMap data = context.getMergedJobDataMap();
        int maxToDelete;
        String runLabel;
        try {
            maxToDelete = JobParams.requireInt(data, KEY_MAX_TO_DELETE);
            runLabel = JobParams.requireString(data, KEY_RUN_LABEL);
        } catch (IllegalArgumentException ex) {
            log.error("TranslationCleanupJob misconfigured: {}", ex.getMessage());
            throw new JobExecutionException("Invalid job parameters: " + ex.getMessage(), ex);
        }

        try {
            cleanupService.purgeRejected(maxToDelete, runLabel);
        } catch (RuntimeException ex) {
            log.error("TranslationCleanupJob failed, will try again at the next scheduled firing", ex);
            JobExecutionException failure = new JobExecutionException("Cleanup failed: " + ex.getMessage(), ex);
            failure.setRefireImmediately(false);
            throw failure;
        }
    }
}

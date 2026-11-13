package com.java.tms.scheduling.job;

import com.java.tms.service.PendingTranslationReminderService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Lab 6 job #2: reminder report about translations still waiting for review.
 *
 * <p>Thin like {@link TranslationCleanupJob}: parameters in, one service call out.
 * It only reads data, so it needs no {@code @DisallowConcurrentExecution}.
 *
 * <p><b>Failure strategy: bounded retry.</b> On a runtime failure the job asks Quartz to refire
 * it immediately, but at most {@value #KEY_MAX_RETRIES} times
 * ({@link JobExecutionContext#getRefireCount()} counts the refires), so a permanently failing
 * service can never cause an infinite loop; after that the failure is recorded and the job
 * waits for its next scheduled firing.
 */
@Slf4j
public class PendingTranslationReminderJob implements Job {

    public static final String KEY_MIN_PENDING = "minPending";
    public static final String KEY_MAX_RETRIES = "maxRetries";

    private final PendingTranslationReminderService reminderService;

    public PendingTranslationReminderJob(PendingTranslationReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap data = context.getMergedJobDataMap();
        int minPending;
        int maxRetries;
        try {
            minPending = JobParams.requireInt(data, KEY_MIN_PENDING);
            maxRetries = JobParams.requireInt(data, KEY_MAX_RETRIES);
        } catch (IllegalArgumentException ex) {
            log.error("PendingTranslationReminderJob misconfigured: {}", ex.getMessage());
            throw new JobExecutionException("Invalid job parameters: " + ex.getMessage(), ex);
        }

        try {
            reminderService.buildReminder(minPending);
        } catch (RuntimeException ex) {
            boolean retry = context.getRefireCount() < maxRetries;
            log.error("PendingTranslationReminderJob failed (refire {} of max {}), {}",
                    context.getRefireCount(), maxRetries, retry ? "retrying now" : "giving up until the next schedule", ex);
            JobExecutionException failure = new JobExecutionException("Reminder failed: " + ex.getMessage(), ex);
            failure.setRefireImmediately(retry);
            throw failure;
        }
    }
}

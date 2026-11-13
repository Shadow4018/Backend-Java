package com.java.tms.scheduling;

import com.java.tms.scheduling.job.PendingTranslationReminderJob;
import com.java.tms.scheduling.job.TranslationCleanupJob;
import com.java.tms.service.PendingTranslationReminderService;
import com.java.tms.service.TranslationCleanupService;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lab 6 scenarios that only mean something when the REAL Quartz engine runs the jobs
 * (an in-memory scheduler, the production {@code JobDetail}s from {@link QuartzConfig}, the
 * real job classes - only their Spring services are Mockito mocks):
 * trigger-over-job JobDataMap precedence, @DisallowConcurrentExecution, and the bounded retry.
 */
class QuartzSchedulerBehaviourTest {

    private final QuartzConfig config = new QuartzConfig();

    @Test
    void valuesFromTheTriggerWinOverJobDefaultsAndMissingKeysKeepTheJobDefault() throws Exception {
        TranslationCleanupService service = mock(TranslationCleanupService.class);
        Scheduler scheduler = QuartzTestSupport.newRamScheduler(new TranslationCleanupJob(service));
        try {
            // job default: maxToDelete=100, runLabel=default (from the production JobDetail)
            scheduler.addJob(config.translationCleanupJobDetail(), true);
            // trigger overrides ONLY maxToDelete
            Trigger trigger = TriggerBuilder.newTrigger()
                    .forJob(QuartzConfig.CLEANUP_JOB_KEY)
                    .withIdentity("merge-test-trigger")
                    .usingJobData(TranslationCleanupJob.KEY_MAX_TO_DELETE, "7")
                    .startNow()
                    .build();
            scheduler.scheduleJob(trigger);
            scheduler.start();

            // 7 = trigger value (wins over default 100); "default" = job value (not overridden)
            verify(service, timeout(5000)).purgeRejected(7, "default");
        } finally {
            scheduler.shutdown(true);
        }
    }

    @Test
    void aSecondFiringWaitsForTheFirstOneBecauseTheCleanupJobIsNonConcurrent() throws Exception {
        TranslationCleanupService service = mock(TranslationCleanupService.class);
        AtomicInteger running = new AtomicInteger();
        AtomicInteger maxObservedInParallel = new AtomicInteger();
        when(service.purgeRejected(anyInt(), anyString())).thenAnswer(invocation -> {
            int now = running.incrementAndGet();
            maxObservedInParallel.accumulateAndGet(now, Math::max);
            Thread.sleep(400); // the "previous run is still in progress" moment
            running.decrementAndGet();
            return 0;
        });

        Scheduler scheduler = QuartzTestSupport.newRamScheduler(new TranslationCleanupJob(service));
        try {
            scheduler.addJob(config.translationCleanupJobDetail(), true);
            // two triggers of the same job become due at the very same moment (threadCount = 3,
            // so without @DisallowConcurrentExecution both runs would overlap)
            scheduler.scheduleJob(TriggerBuilder.newTrigger().forJob(QuartzConfig.CLEANUP_JOB_KEY)
                    .withIdentity("parallel-a").startNow().build());
            scheduler.scheduleJob(TriggerBuilder.newTrigger().forJob(QuartzConfig.CLEANUP_JOB_KEY)
                    .withIdentity("parallel-b").startNow().build());
            scheduler.start();

            verify(service, timeout(10_000).times(2)).purgeRejected(anyInt(), anyString());

            assertThat(maxObservedInParallel.get()).isEqualTo(1); // never two at once
        } finally {
            scheduler.shutdown(true);
        }
    }

    @Test
    void aPermanentlyFailingReminderIsRetriedAtMostMaxRetriesTimesAndThenGivesUp() throws Exception {
        PendingTranslationReminderService service = mock(PendingTranslationReminderService.class);
        when(service.buildReminder(anyInt())).thenThrow(new IllegalStateException("db down"));

        Scheduler scheduler = QuartzTestSupport.newRamScheduler(new PendingTranslationReminderJob(service));
        try {
            scheduler.addJob(config.pendingReminderJobDetail(), true); // maxRetries default = 2
            scheduler.scheduleJob(TriggerBuilder.newTrigger().forJob(QuartzConfig.REMINDER_JOB_KEY)
                    .withIdentity("retry-test").startNow().build());
            scheduler.start();

            // 1 original run + 2 refires = 3 calls...
            verify(service, timeout(5000).times(3)).buildReminder(anyInt());
            // ...and then it STOPS: no endless loop
            Thread.sleep(700);
            verify(service, times(3)).buildReminder(anyInt());
        } finally {
            scheduler.shutdown(true);
        }
    }

    @Test
    void aMisconfiguredJobNeverReachesTheServiceAndIsNotRetried() throws Exception {
        PendingTranslationReminderService service = mock(PendingTranslationReminderService.class);

        Scheduler scheduler = QuartzTestSupport.newRamScheduler(new PendingTranslationReminderJob(service));
        try {
            scheduler.addJob(config.pendingReminderJobDetail(), true);
            // a trigger override that makes a required parameter unusable (not a number)
            scheduler.scheduleJob(TriggerBuilder.newTrigger().forJob(QuartzConfig.REMINDER_JOB_KEY)
                    .withIdentity("broken-trigger")
                    .usingJobData(PendingTranslationReminderJob.KEY_MIN_PENDING, "not-a-number")
                    .startNow().build());
            scheduler.start();

            Thread.sleep(1000);

            verify(service, never()).buildReminder(anyInt());
        } finally {
            scheduler.shutdown(true);
        }
    }
}

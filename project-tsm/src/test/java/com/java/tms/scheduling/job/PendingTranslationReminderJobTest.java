package com.java.tms.scheduling.job;

import com.java.tms.service.PendingTranslationReminderService;
import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Lab 6: the reminder job is thin, rejects bad parameters, and uses the BOUNDED-RETRY failure
 * strategy: it asks Quartz to refire only while {@code refireCount < maxRetries}.
 */
class PendingTranslationReminderJobTest {

    private final PendingTranslationReminderService service = mock(PendingTranslationReminderService.class);
    private final PendingTranslationReminderJob job = new PendingTranslationReminderJob(service);

    private JobExecutionContext context(JobDataMap data, int refireCount) {
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(data);
        when(context.getRefireCount()).thenReturn(refireCount);
        return context;
    }

    private JobDataMap validData() {
        JobDataMap data = new JobDataMap();
        data.put(PendingTranslationReminderJob.KEY_MIN_PENDING, "3");
        data.put(PendingTranslationReminderJob.KEY_MAX_RETRIES, "2");
        return data;
    }

    @Test
    void readsItsParametersAndMakesExactlyOneServiceCall() throws Exception {
        job.execute(context(validData(), 0));

        verify(service).buildReminder(3);
        verifyNoMoreInteractions(service);
    }

    @Test
    void aMissingKeyFailsExplicitlyAndNeverCallsTheService() {
        JobDataMap data = new JobDataMap();
        data.put(PendingTranslationReminderJob.KEY_MAX_RETRIES, "2"); // minPending absent

        assertThatThrownBy(() -> job.execute(context(data, 0)))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageContaining("minPending");
        verify(service, never()).buildReminder(anyInt());
    }

    @Test
    void aFailureIsRetriedWhileTheRefireCountIsBelowTheLimit() {
        when(service.buildReminder(anyInt())).thenThrow(new IllegalStateException("db down"));

        for (int refires = 0; refires < 2; refires++) {
            int current = refires;
            assertThatThrownBy(() -> job.execute(context(validData(), current)))
                    .isInstanceOfSatisfying(JobExecutionException.class,
                            ex -> assertThat(ex.refireImmediately()).isTrue());
        }
    }

    @Test
    void onceTheRetryLimitIsReachedItGivesUpInsteadOfLoopingForever() {
        when(service.buildReminder(anyInt())).thenThrow(new IllegalStateException("db down"));

        // refireCount == maxRetries (2): this was already the last allowed attempt
        assertThatThrownBy(() -> job.execute(context(validData(), 2)))
                .isInstanceOfSatisfying(JobExecutionException.class,
                        ex -> assertThat(ex.refireImmediately()).isFalse());
    }
}

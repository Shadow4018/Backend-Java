package com.java.tms.scheduling.job;

import com.java.tms.service.TranslationCleanupService;
import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Lab 6: "jobs are thin", "a misspelled or missing key is an explicit error" and the cleanup
 * job's failure strategy ("record the failure and wait for the next scheduled firing").
 */
class TranslationCleanupJobTest {

    private final TranslationCleanupService service = mock(TranslationCleanupService.class);
    private final TranslationCleanupJob job = new TranslationCleanupJob(service);

    private JobExecutionContext contextWith(JobDataMap data) {
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(data);
        return context;
    }

    private JobDataMap validData() {
        JobDataMap data = new JobDataMap();
        data.put(TranslationCleanupJob.KEY_MAX_TO_DELETE, "25");
        data.put(TranslationCleanupJob.KEY_RUN_LABEL, "unit-test");
        return data;
    }

    @Test
    void readsItsParametersAndMakesExactlyOneServiceCall() throws Exception {
        job.execute(contextWith(validData()));

        verify(service).purgeRejected(25, "unit-test");
        verifyNoMoreInteractions(service); // nothing else: no business logic inside the job
    }

    @Test
    void aMisspelledKeyFailsExplicitlyAndNeverCallsTheService() {
        JobDataMap data = new JobDataMap();
        data.put("maxToDelet", "25"); // typo: missing the final 'e'
        data.put(TranslationCleanupJob.KEY_RUN_LABEL, "unit-test");

        assertThatThrownBy(() -> job.execute(contextWith(data)))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageContaining("maxToDelete");
        verify(service, never()).purgeRejected(anyInt(), anyString());
    }

    @Test
    void aMissingKeyFailsExplicitly() {
        JobDataMap data = new JobDataMap();
        data.put(TranslationCleanupJob.KEY_MAX_TO_DELETE, "25"); // runLabel is absent

        assertThatThrownBy(() -> job.execute(contextWith(data)))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageContaining("runLabel");
        verify(service, never()).purgeRejected(anyInt(), anyString());
    }

    @Test
    void aNonNumericValueFailsExplicitly() {
        JobDataMap data = validData();
        data.put(TranslationCleanupJob.KEY_MAX_TO_DELETE, "lots");

        assertThatThrownBy(() -> job.execute(contextWith(data)))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageContaining("must be an integer");
        verify(service, never()).purgeRejected(anyInt(), anyString());
    }

    @Test
    void aServiceFailureIsRecordedAndNotRetriedImmediately() {
        when(service.purgeRejected(anyInt(), anyString())).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> job.execute(contextWith(validData())))
                .isInstanceOfSatisfying(JobExecutionException.class, ex -> {
                    assertThat(ex.refireImmediately()).isFalse(); // wait for the next scheduled run
                    assertThat(ex).hasMessageContaining("db down");
                });
        verify(service).purgeRejected(25, "unit-test"); // tried exactly once
    }
}

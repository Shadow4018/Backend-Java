package com.java.tms.scheduling;

import com.java.tms.scheduling.job.PendingTranslationReminderJob;
import com.java.tms.scheduling.job.TranslationCleanupJob;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Trigger;

import java.util.List;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lab 6, scenarios "jobs and triggers are declared in one place", "every trigger has a defined
 * misfire instruction", "cron triggers do not depend on the server time zone" and
 * "concurrent execution is forbidden where it can do harm". Pure unit test: the
 * {@code @Configuration} methods are called directly, no Spring context needed.
 */
class QuartzConfigTest {

    private static final String HOURLY_CRON = "0 0 * * * ?";

    private final QuartzConfig config = new QuartzConfig();

    private final JobDetail cleanupJob = config.translationCleanupJobDetail();
    private final JobDetail reminderJob = config.pendingReminderJobDetail();
    private final CronTrigger hourly = config.translationCleanupHourlyTrigger(HOURLY_CRON);
    private final CronTrigger nightly = config.translationCleanupNightlyTrigger();
    private final CronTrigger reminder = config.pendingReminderTrigger();

    // ---- one place, JobKey wiring, explicit name + group -------------------------------------

    @Test
    void everyTriggerPointsAtItsJobByJobKey() {
        assertThat(hourly.getJobKey()).isEqualTo(cleanupJob.getKey());
        assertThat(nightly.getJobKey()).isEqualTo(cleanupJob.getKey());
        assertThat(reminder.getJobKey()).isEqualTo(reminderJob.getKey());
    }

    @Test
    void everyJobAndTriggerHasAnExplicitNameAndGroup() {
        List<String> names = List.of(
                cleanupJob.getKey().getName(), cleanupJob.getKey().getGroup(),
                reminderJob.getKey().getName(), reminderJob.getKey().getGroup(),
                hourly.getKey().getName(), hourly.getKey().getGroup(),
                nightly.getKey().getName(), nightly.getKey().getGroup(),
                reminder.getKey().getName(), reminder.getKey().getGroup());

        assertThat(names).allSatisfy(n -> assertThat(n).isNotBlank());
        // Quartz would silently fill in "DEFAULT" as the group and a random UUID as the name.
        assertThat(names).doesNotContain("DEFAULT");
    }

    @Test
    void theCleanupJobHasTwoTriggersWithDistinctKeys() {
        List<CronTrigger> cleanupTriggers = List.of(hourly, nightly, reminder).stream()
                .filter(t -> t.getJobKey().equals(cleanupJob.getKey()))
                .toList();

        assertThat(cleanupTriggers).hasSize(2);
        assertThat(hourly.getKey()).isNotEqualTo(nightly.getKey());
    }

    // ---- JobDataMap defaults vs trigger overrides (as declared) ------------------------------

    @Test
    void jobsCarryTheirDefaultsInTheJobDataMap() {
        assertThat(cleanupJob.getJobDataMap().getString(TranslationCleanupJob.KEY_MAX_TO_DELETE)).isEqualTo("100");
        assertThat(cleanupJob.getJobDataMap().getString(TranslationCleanupJob.KEY_RUN_LABEL)).isEqualTo("default");
        assertThat(reminderJob.getJobDataMap().getString(PendingTranslationReminderJob.KEY_MIN_PENDING)).isEqualTo("1");
        assertThat(reminderJob.getJobDataMap().getString(PendingTranslationReminderJob.KEY_MAX_RETRIES)).isEqualTo("2");
    }

    @Test
    void triggersOverrideOnlySomeOfTheJobKeys() {
        // hourly: overrides runLabel only -> maxToDelete is NOT in its own map (job default applies)
        assertThat(hourly.getJobDataMap().containsKey(TranslationCleanupJob.KEY_MAX_TO_DELETE)).isFalse();
        assertThat(hourly.getJobDataMap().getString(TranslationCleanupJob.KEY_RUN_LABEL)).isEqualTo("hourly-sweep");

        // nightly: overrides both
        assertThat(nightly.getJobDataMap().getString(TranslationCleanupJob.KEY_MAX_TO_DELETE)).isEqualTo("1000");
        assertThat(nightly.getJobDataMap().getString(TranslationCleanupJob.KEY_RUN_LABEL)).isEqualTo("nightly-deep-clean");

        // reminder: overrides minPending only -> maxRetries keeps the job default
        assertThat(reminder.getJobDataMap().getString(PendingTranslationReminderJob.KEY_MIN_PENDING)).isEqualTo("2");
        assertThat(reminder.getJobDataMap().containsKey(PendingTranslationReminderJob.KEY_MAX_RETRIES)).isFalse();
    }

    // ---- misfire instructions ----------------------------------------------------------------

    @Test
    void everyTriggerHasAnExplicitMisfireInstructionAndTheTestPinsIt() {
        assertThat(hourly.getMisfireInstruction()).isEqualTo(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
        assertThat(nightly.getMisfireInstruction()).isEqualTo(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
        assertThat(reminder.getMisfireInstruction()).isEqualTo(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);

        // None of them relies on Quartz's implicit "smart policy".
        assertThat(List.of(hourly, nightly, reminder))
                .allSatisfy(t -> assertThat(t.getMisfireInstruction()).isNotEqualTo(Trigger.MISFIRE_INSTRUCTION_SMART_POLICY));
    }

    // ---- time zones --------------------------------------------------------------------------

    @Test
    void everyCronTriggerHasAnExplicitTimeZoneThatDoesNotDependOnTheServer() {
        assertThat(hourly.getTimeZone().getID()).isEqualTo("Europe/Kyiv");
        assertThat(nightly.getTimeZone().getID()).isEqualTo("Europe/Kyiv");
        assertThat(reminder.getTimeZone().getID()).isEqualTo("Europe/Kyiv");
    }

    @Test
    void anUnknownTimeZoneIdSilentlyBecomesGmt_whichIsWhyTheConfigUsesZoneIdOf() {
        // Documents the trap named in the lab: TimeZone.getTimeZone never fails on a typo...
        assertThat(TimeZone.getTimeZone("Europe/Kyyiv").getID()).isEqualTo("GMT");
        // ...so QuartzConfig builds the zone via ZoneId.of, which would throw on a typo instead.
        assertThat(QuartzConfig.businessTimeZone().getID()).isNotEqualTo("GMT");
    }

    @Test
    void theHourlyCronExpressionComesFromTheArgumentSoItCanBeOverriddenForDemos() {
        CronTrigger everyTwentySeconds = config.translationCleanupHourlyTrigger("0/20 * * * * ?");

        assertThat(everyTwentySeconds.getCronExpression()).isEqualTo("0/20 * * * * ?");
        assertThat(hourly.getCronExpression()).isEqualTo(HOURLY_CRON);
    }

    // ---- concurrency -------------------------------------------------------------------------

    @Test
    void theDataMutatingCleanupJobForbidsConcurrentExecutionButTheReadOnlyReminderDoesNot() {
        // "Exection" is Quartz's own spelling of the method name.
        assertThat(cleanupJob.isConcurrentExecutionDisallowed()).isTrue();
        assertThat(reminderJob.isConcurrentExecutionDisallowed()).isFalse();
    }

    @Test
    void jobsAreDurableSoTheyStayInTheStoreEvenWithoutTriggers() {
        assertThat(cleanupJob.isDurable()).isTrue();
        assertThat(reminderJob.isDurable()).isTrue();
    }
}

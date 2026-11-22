package com.java.tms.scheduling;

import com.java.tms.scheduling.job.PendingTranslationReminderJob;
import com.java.tms.scheduling.job.TranslationCleanupJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Lab 6: the ONE place where every Quartz job and trigger is declared.
 *
 * <p>Spring Boot's Quartz auto-configuration picks up all {@link JobDetail} and
 * {@code Trigger} beans and registers them in the JDBC job store at startup. Because every
 * job/trigger has a fixed name+group and {@code spring.quartz.overwrite-existing-jobs=true},
 * a restart or a new deployment <b>updates</b> the stored definitions instead of creating
 * duplicates, and the persisted schedule survives restarts.
 *
 * <pre>
 *   job  translation-cleanup  (group tms-maintenance)   - TWO triggers:
 *        trigger translation-cleanup-hourly   cron hourly,  keeps the job's default maxToDelete
 *        trigger translation-cleanup-nightly  cron 03:00,   overrides maxToDelete (deep clean)
 *   job  pending-reminder     (group tms-reminders)     - one trigger:
 *        trigger pending-reminder-weekdays    cron Mon-Fri 09:00, overrides minPending
 * </pre>
 *
 * <p>All cron triggers carry an explicit time zone and an explicit misfire instruction, so
 * neither depends on the server's locale or on Quartz's implicit "smart policy".
 */
@Configuration
public class QuartzConfig {

    public static final String MAINTENANCE_GROUP = "tms-maintenance";
    public static final String REMINDERS_GROUP = "tms-reminders";

    public static final JobKey CLEANUP_JOB_KEY = JobKey.jobKey("translation-cleanup", MAINTENANCE_GROUP);
    public static final JobKey REMINDER_JOB_KEY = JobKey.jobKey("pending-reminder", REMINDERS_GROUP);

    public static final TriggerKey CLEANUP_HOURLY_TRIGGER_KEY =
            TriggerKey.triggerKey("translation-cleanup-hourly", MAINTENANCE_GROUP);
    public static final TriggerKey CLEANUP_NIGHTLY_TRIGGER_KEY =
            TriggerKey.triggerKey("translation-cleanup-nightly", MAINTENANCE_GROUP);
    public static final TriggerKey REMINDER_TRIGGER_KEY =
            TriggerKey.triggerKey("pending-reminder-weekdays", REMINDERS_GROUP);

    /** Business time zone of the team; never the server's default zone. */
    public static final String BUSINESS_TIME_ZONE = "Europe/Kyiv";

    // ---- job defaults (JobDataMap on the JobDetail; triggers may override them) -------------

    public static final String DEFAULT_MAX_TO_DELETE = "100";
    public static final String DEFAULT_CLEANUP_LABEL = "default";
    public static final String DEFAULT_MIN_PENDING = "1";
    public static final String DEFAULT_MAX_RETRIES = "2";

    // ---- jobs --------------------------------------------------------------------------------

    @Bean
    public JobDetail translationCleanupJobDetail() {
        return JobBuilder.newJob(TranslationCleanupJob.class)
                .withIdentity(CLEANUP_JOB_KEY)
                .withDescription("Deletes REJECTED translations (idempotent, batch-limited)")
                .usingJobData(TranslationCleanupJob.KEY_MAX_TO_DELETE, DEFAULT_MAX_TO_DELETE)
                .usingJobData(TranslationCleanupJob.KEY_RUN_LABEL, DEFAULT_CLEANUP_LABEL)
                .storeDurably()
                .build();
    }

    @Bean
    public JobDetail pendingReminderJobDetail() {
        return JobBuilder.newJob(PendingTranslationReminderJob.class)
                .withIdentity(REMINDER_JOB_KEY)
                .withDescription("Reports projects with translations waiting for review (read-only)")
                .usingJobData(PendingTranslationReminderJob.KEY_MIN_PENDING, DEFAULT_MIN_PENDING)
                .usingJobData(PendingTranslationReminderJob.KEY_MAX_RETRIES, DEFAULT_MAX_RETRIES)
                .storeDurably()
                .build();
    }

    // ---- triggers ----------------------------------------------------------------------------

    /**
     * Trigger #1 of the cleanup job. Overrides only {@code runLabel}: {@code maxToDelete}
     * keeps the job default. Misfire = DO_NOTHING: a missed hourly sweep is not worth a
     * catch-up run, the next hour does the same work anyway.
     */
    @Bean
    public CronTrigger translationCleanupHourlyTrigger(
            @Value("${app.quartz.cleanup-hourly-cron:0 0 * * * ?}") String cron) {
        return TriggerBuilder.newTrigger()
                .forJob(CLEANUP_JOB_KEY)
                .withIdentity(CLEANUP_HOURLY_TRIGGER_KEY)
                .withDescription("Hourly light sweep")
                .usingJobData(TranslationCleanupJob.KEY_RUN_LABEL, "hourly-sweep")
                .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                        .inTimeZone(businessTimeZone())
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    /**
     * Trigger #2 of the SAME cleanup job: nightly deep clean. Overrides both keys with its own
     * values. Misfire = FIRE_ONCE_NOW: if the app was down at 03:00, run the deep clean once
     * as soon as it is back.
     */
    @Bean
    public CronTrigger translationCleanupNightlyTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(CLEANUP_JOB_KEY)
                .withIdentity(CLEANUP_NIGHTLY_TRIGGER_KEY)
                .withDescription("Nightly deep clean")
                .usingJobData(TranslationCleanupJob.KEY_MAX_TO_DELETE, "1000")
                .usingJobData(TranslationCleanupJob.KEY_RUN_LABEL, "nightly-deep-clean")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 3 * * ?")
                        .inTimeZone(businessTimeZone())
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();
    }

    /**
     * Reminder every weekday at 09:00 Kyiv time. Overrides {@code minPending} (default 1) with
     * 2: a single waiting translation is not worth a reminder. Misfire = DO_NOTHING: a stale
     * morning reminder sent in the afternoon is just noise.
     */
    @Bean
    public CronTrigger pendingReminderTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(REMINDER_JOB_KEY)
                .withIdentity(REMINDER_TRIGGER_KEY)
                .withDescription("Weekday 09:00 reminder")
                .usingJobData(PendingTranslationReminderJob.KEY_MIN_PENDING, "2")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 9 ? * MON-FRI")
                        .inTimeZone(businessTimeZone())
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    /**
     * {@link ZoneId#of(String)} fails fast on an unknown id, whereas
     * {@code TimeZone.getTimeZone("typo")} would silently fall back to GMT.
     */
    static TimeZone businessTimeZone() {
        return TimeZone.getTimeZone(ZoneId.of(BUSINESS_TIME_ZONE));
    }
}

package com.java.tms.scheduling;

import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lab 6, scenario "the schedule is stored in the database via Liquibase": a full Spring context
 * (H2, JDBC job store, scheduler NOT auto-started so real cron triggers cannot fire mid-test)
 * proves the QRTZ_* tables come from Liquibase, Boot is told never to create them, and the
 * jobs/triggers declared in {@link QuartzConfig} really live in those tables.
 */
@SpringBootTest
@ActiveProfiles("test")
class QuartzPersistenceTest {

    @Autowired
    private Scheduler scheduler;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private Environment environment;

    @Test
    void springBootIsConfiguredNeverToCreateTheQuartzTablesItself() {
        assertThat(environment.getProperty("spring.quartz.job-store-type")).isEqualTo("jdbc");
        assertThat(environment.getProperty("spring.quartz.jdbc.initialize-schema")).isEqualTo("never");
    }

    @Test
    void theQuartzTablesWereCreatedByLiquibaseChangesets() {
        Integer quartzChangeSets = jdbc.queryForObject(
                "select count(*) from DATABASECHANGELOG where ID like '006-create-qrtz-%'", Integer.class);

        assertThat(quartzChangeSets).isEqualTo(11); // one changeset per QRTZ_* table
    }

    @Test
    void jobsAndTriggersFromTheConfigurationClassAreStoredInTheDatabase() throws Exception {
        // filtered by this context's scheduler name (every test context has its own, see application-test.yml)
        String schedName = scheduler.getSchedulerName();
        Integer jobRows = jdbc.queryForObject(
                "select count(*) from QRTZ_JOB_DETAILS where SCHED_NAME = ?", Integer.class, schedName);
        Integer triggerRows = jdbc.queryForObject(
                "select count(*) from QRTZ_TRIGGERS where SCHED_NAME = ?", Integer.class, schedName);
        Integer cronRowsInKyivTime = jdbc.queryForObject(
                "select count(*) from QRTZ_CRON_TRIGGERS where SCHED_NAME = ? and TIME_ZONE_ID = 'Europe/Kyiv'",
                Integer.class, schedName);

        assertThat(jobRows).isEqualTo(2);
        assertThat(triggerRows).isEqualTo(3);
        assertThat(cronRowsInKyivTime).isEqualTo(3);
    }

    @Test
    void theStoredScheduleCarriesTheCleanupJobsTwoTriggersAndTheirOverrides() throws Exception {
        List<? extends Trigger> cleanupTriggers = scheduler.getTriggersOfJob(QuartzConfig.CLEANUP_JOB_KEY);
        CronTrigger nightly = (CronTrigger) scheduler.getTrigger(QuartzConfig.CLEANUP_NIGHTLY_TRIGGER_KEY);

        assertThat(cleanupTriggers).extracting(Trigger::getKey)
                .containsExactlyInAnyOrder(QuartzConfig.CLEANUP_HOURLY_TRIGGER_KEY, QuartzConfig.CLEANUP_NIGHTLY_TRIGGER_KEY);
        // read back FROM THE DATABASE (not from the bean): proves JobDataMap + misfire survive storage
        assertThat(nightly.getJobDataMap().getString("maxToDelete")).isEqualTo("1000");
        assertThat(nightly.getMisfireInstruction()).isEqualTo(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
        assertThat(scheduler.getJobDetail(QuartzConfig.CLEANUP_JOB_KEY).getJobDataMap().getString("maxToDelete"))
                .isEqualTo("100");
    }

    @Test
    void theStoredNonConcurrentFlagMatchesTheJobAnnotation() throws Exception {
        assertThat(scheduler.getJobDetail(QuartzConfig.CLEANUP_JOB_KEY).isConcurrentExecutionDisallowed()).isTrue();
        assertThat(scheduler.getJobDetail(QuartzConfig.REMINDER_JOB_KEY).isConcurrentExecutionDisallowed()).isFalse();
    }
}

package com.java.tms.scheduling;

import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.JobFactory;

import java.util.Properties;
import java.util.UUID;

/**
 * Builds a throw-away, in-memory (RAMJobStore) Quartz scheduler for tests that must see Quartz's
 * REAL behaviour (JobDataMap merging, non-concurrent execution, refire counting) without a
 * database. The job instances are created by the given factory so the real job classes can be
 * wired to Mockito mocks of their service.
 */
final class QuartzTestSupport {

    private QuartzTestSupport() {
    }

    static Scheduler newRamScheduler(Job job) throws SchedulerException {
        Properties props = new Properties();
        // unique name per scheduler: Quartz keeps a JVM-wide registry keyed by name
        props.setProperty("org.quartz.scheduler.instanceName", "test-" + UUID.randomUUID());
        props.setProperty("org.quartz.scheduler.instanceId", "AUTO");
        props.setProperty("org.quartz.threadPool.threadCount", "3");
        props.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        Scheduler scheduler = new StdSchedulerFactory(props).getScheduler();
        JobFactory factory = (bundle, sched) -> job;
        scheduler.setJobFactory(factory);
        return scheduler;
    }
}

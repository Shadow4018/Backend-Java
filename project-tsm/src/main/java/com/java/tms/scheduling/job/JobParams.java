package com.java.tms.scheduling.job;

import org.quartz.JobDataMap;

/**
 * Strict readers for Quartz {@link JobDataMap} values (Lab 6, "a misspelled or missing key must
 * fail loudly" scenario). Because the JDBC job store runs with {@code useProperties=true}, every
 * value is a String, so numbers are parsed here.
 *
 * <p>Deliberate decision: <b>no silent defaults at read time</b>. The defaults live in the
 * {@code JobDetail}'s JobDataMap ({@code QuartzConfig}); if a key is nevertheless absent
 * (typo in a trigger override, hand-edited row...) the job fails with a clear message instead of
 * quietly running with a guessed value.
 */
public final class JobParams {

    private JobParams() {
    }

    public static String requireString(JobDataMap data, String key) {
        Object value = data.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("Missing job parameter '" + key
                    + "'; keys present: " + java.util.Arrays.toString(data.getKeys()));
        }
        return value.toString();
    }

    public static int requireInt(JobDataMap data, String key) {
        String raw = requireString(data, key);
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Job parameter '" + key + "' must be an integer but was '" + raw + "'", ex);
        }
    }
}

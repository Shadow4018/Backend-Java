package com.java.tms.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The one object the services share: the JSON error body every service answers with.
 */
class ApplicationErrorResponseTest {

    @Test
    void buildsTheBodyFromAnExceptionAndAnHttpStatus() {
        ApplicationErrorResponse response =
                new ApplicationErrorResponse(new IllegalArgumentException("Project not found: 42"), 400);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getError()).isEqualTo("IllegalArgumentException");
        assertThat(response.getMessage()).isEqualTo("Project not found: 42");
        assertThat(response.getTimestamp()).isNotNull();
    }
}

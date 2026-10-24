package com.java.tms.aop.selfinvocation;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.java.tms.aop.aspect.TimingAspect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lab 4, scenarios 1-4 from the PDF, proven end-to-end against a REAL Spring context (a
 * plain unit test cannot demonstrate self-invocation: the whole phenomenon only exists
 * because of the AOP proxy Spring builds around the bean). {@link #demoService} here is
 * that proxy, autowired exactly as any other bean would receive it.
 *
 * <p>Whether {@link TimingAspect} actually ran is observed the same way a human would check
 * it — by looking at the log output — using a Logback {@link ListAppender} attached directly
 * to the aspect's logger for the duration of each test.
 */
@SpringBootTest
@ActiveProfiles("test")
class SelfInvocationDemoServiceTest {

    @Autowired
    private SelfInvocationDemoService demoService;

    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachLogAppender() {
        appender = new ListAppender<>();
        appender.start();
        timingAspectLogger().addAppender(appender);
    }

    @AfterEach
    void detachLogAppender() {
        timingAspectLogger().detachAppender(appender);
    }

    /**
     * Scenario "Зовнішній виклик правильно активує аспект": the test itself is an outside
     * caller invoking the method on the Spring-managed proxy.
     */
    @Test
    void externalCallThroughTheProxyTriggersTheAspect() {
        String result = demoService.timedOperation();

        assertThat(result).isEqualTo("timed-operation-result");
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage()).contains("SelfInvocationDemoService#timedOperation");
    }

    /**
     * Scenario "Самостійний виклик (self-invocation) НЕ активує аспект": the annotated
     * method is called via a plain unqualified call from inside the very same class.
     */
    @Test
    void selfInvocationBypassesTheProxyAndTheAspectNeverFires() {
        String result = demoService.viaSelfInvocation();

        // The underlying method still runs and returns its real value — only the aspect
        // around it is skipped, silently, which is exactly the pitfall being demonstrated.
        assertThat(result).isEqualTo("timed-operation-result");
        assertThat(appender.list).isEmpty();
    }

    /**
     * Scenario "GIVEN: він викликається з іншого біна": shows the aspect firing again when
     * invoked through the proxy handed to a (simulated) different caller.
     */
    @Test
    void callingOnTheInjectedProxyFromAnotherCallerStillTriggersTheAspect() {
        String result = demoService.viaExternalCall(demoService);

        assertThat(result).isEqualTo("timed-operation-result");
        assertThat(appender.list).hasSize(1);
    }

    private Logger timingAspectLogger() {
        return (Logger) LoggerFactory.getLogger(TimingAspect.class);
    }
}

package com.java.tms.aop.selfinvocation;

import com.java.tms.aop.annotation.ExecutionTimed;
import com.java.tms.aop.aspect.TimingAspect;
import org.springframework.stereotype.Service;

/**
 * Lab 4, mandatory part 2: a deliberate, documented demonstration of the classic Spring AOP
 * self-invocation pitfall. This class is not meant to model any real TMS behaviour — its
 * only job is to make the pitfall unmistakable and reproducible in a test.
 *
 * <p>Spring AOP proxies a bean, not its class: advice only runs when a call arrives
 * <em>through that proxy</em>. {@link #timedOperation()} is annotated with
 * {@link ExecutionTimed}, so {@link TimingAspect} fires whenever some other object calls
 * {@code someInstance.timedOperation()} on the Spring-managed proxy — but a plain,
 * unqualified call to {@code timedOperation()} from another method of this same class
 * resolves directly against {@code this} (the raw target object inside the proxy), never
 * passing back through the proxy. The aspect then silently does not run — no exception, no
 * log entry, nothing. That silent non-firing is the intended lesson here, not a bug.
 */
@Service
public class SelfInvocationDemoService {

    /**
     * threshold = 0 ms makes every single call land in the WARN branch of
     * {@link TimingAspect}, so whenever the aspect does run, it is unmistakable in the logs.
     */
    @ExecutionTimed(slowThresholdMs = 0)
    public String timedOperation() {
        return "timed-operation-result";
    }

    /**
     * Self-invocation path: an unqualified call to {@code timedOperation()} from inside this
     * class bypasses the Spring AOP proxy entirely, so {@link TimingAspect} does NOT run for
     * this call.
     */
    public String viaSelfInvocation() {
        return timedOperation();
    }

    /**
     * External-call path: some other object (e.g. a different bean, or a test) calls
     * {@code timedOperation()} on the proxy it was handed, so {@link TimingAspect} DOES run.
     * {@code self} is deliberately passed in rather than looked up, to make explicit that
     * what matters is which object the call is made on, not which class the code lives in.
     */
    public String viaExternalCall(SelfInvocationDemoService self) {
        return self.timedOperation();
    }
}

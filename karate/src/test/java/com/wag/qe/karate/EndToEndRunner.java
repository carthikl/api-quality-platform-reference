package com.wag.qe.karate;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JUnit 5 runner for the E2E prescription checkout journey.
 *
 * Runs sequentially (parallel = 1) because each scenario chains the output of
 * one microservice call as input to the next. Parallel execution would
 * interleave scenario state and produce non-deterministic failures.
 *
 * Report is written to target/karate-reports alongside the standard Karate output.
 *
 * Gated by the system property -Drun.e2e=true so that a plain `mvn test -pl karate`
 * does not include E2E tests in the PR pipeline. E2E runs only when explicitly
 * requested — on push to main via the pr-quality-gate workflow, and on staging
 * deploy via the staging-smoke workflow.
 *
 * Runtime invocation:
 *   mvn test -pl karate -Dtest=EndToEndRunner -Drun.e2e=true \
 *            -Dapi.base.url=https://staging-api.walgreens.com \
 *            -Dkarate.env=staging
 */
class EndToEndRunner {

    @Test
    void testSequential() {
        // Skip unless the caller explicitly opts in to E2E execution.
        // This prevents the journey test from running on every PR — E2E is a
        // staging confidence gate, not a PR regression gate.
        Assumptions.assumeTrue(
            "true".equalsIgnoreCase(System.getProperty("run.e2e", "false")),
            "E2E journey skipped — set -Drun.e2e=true to enable"
        );

        Results results = Runner.path("classpath:features/e2e")
                .tags("@e2e")
                .parallel(1);

        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}

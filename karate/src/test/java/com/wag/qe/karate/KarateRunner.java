package com.wag.qe.karate;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JUnit 5 runner for Karate scenarios.
 *
 * Default behaviour: runs all @smoke-tagged scenarios in parallel across 5 threads.
 * HTML report is written to target/karate-reports automatically.
 *
 * Runtime overrides:
 *   -Dkarate.options="--tags @prescription"   run a specific tag
 *   -Dkarate.options="--tags ~@smoke"          exclude smoke, run everything else
 *   -Dkarate.env=staging                       switch environment block in karate-config.js
 */
class KarateRunner {

    @Test
    void testParallel() {
        // When karate.options is not set, default to @smoke so the runner is safe
        // to trigger from a post-deploy hook without running the full suite.
        String karateOptions = System.getProperty("karate.options", "--tags @smoke");
        String tags = karateOptions.replaceAll("--tags\\s*", "").trim();

        Results results = Runner.path("classpath:features")
                .tags(tags)
                .parallel(5);

        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}

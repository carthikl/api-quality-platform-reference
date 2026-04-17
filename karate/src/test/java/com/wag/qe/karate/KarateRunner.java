package com.wag.qe.karate;

import com.intuit.karate.junit5.Karate;

/**
 * JUnit 5 entry point for Surefire discovery.
 * Karate resolves all .feature files under src/test/resources from the classpath.
 * Tag filtering via: -Dkarate.options="--tags @smoke"
 */
class KarateRunner {

    @Karate.Test
    Karate testAll() {
        return Karate.run().relativeTo(getClass());
    }
}

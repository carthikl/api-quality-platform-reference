package com.wag.qe.pact.provider;

import au.com.dius.pact.provider.junit5.HttpsTestTarget;
import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URL;

/**
 * Provider-side verification: PatientService satisfying the contracts defined by its consumers.
 *
 * If this test fails, PatientService has broken the contract PrescriptionService depends on.
 * Fix the provider — not the consumer.
 *
 * What this test does:
 *   Pact reads target/pacts/PrescriptionService-PatientService.json, replays every interaction
 *   declared in it against the real PatientService (JSONPlaceholder here, the actual service
 *   in CI), and asserts the response matches the types and structure PrescriptionService declared.
 *   No consumer code runs. No mock. This is a real HTTP call to a real provider.
 *
 * In production CI, replace @PactFolder with @PactBroker to pull contracts published by
 * every registered consumer. The broker tracks verification results per provider version,
 * enabling "can-i-deploy" checks before any environment promotion.
 */
@Provider("PatientService")
@PactFolder("target/pacts")
class PatientProviderTest {

    @BeforeEach
    void configureTarget(PactVerificationContext context) throws Exception {
        String baseUrl = System.getProperty("api.base.url", "https://jsonplaceholder.typicode.com");
        URL url = new URL(baseUrl);

        // fromUrl() reads host, port, and path from the URL and picks the right protocol.
        // HttpsTestTarget for TLS (prod/staging/JSONPlaceholder), HttpTestTarget for plain HTTP.
        if ("https".equals(url.getProtocol())) {
            context.setTarget(HttpsTestTarget.fromUrl(url));
        } else {
            context.setTarget(HttpTestTarget.fromUrl(url));
        }
    }

    @State("patient with ID 1 exists")
    void patientWithId1Exists() {
        // JSONPlaceholder permanently has user ID 1 — no test data setup required here.
        // Against a real PatientService, this method would insert the test patient into
        // the provider's test database before Pact replays the interaction.
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPatientServiceContracts(PactVerificationContext context) {
        context.verifyInteraction();
    }
}

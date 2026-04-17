package com.wag.qe.pact.consumer;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Consumer-side contract: Prescription Service consuming Patient API.
 * Defines the minimum response shape Prescription Service depends on.
 * Generates target/pacts/PrescriptionService-PatientService.json.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "PatientService")
class PrescriptionConsumerTest {

    // stub — will be expanded in next phase
}

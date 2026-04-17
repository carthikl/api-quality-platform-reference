package com.wag.qe.pact.consumer;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This test defines what PrescriptionService expects from PatientService.
 * The generated pact file is the contract. PatientService must verify it can
 * satisfy this contract before deploying.
 *
 * How consumer-driven contract testing works in practice:
 *
 *   1. The @Pact method below declares the interaction: "when PrescriptionService
 *      calls GET /users/1, it expects a 200 response containing at minimum these
 *      four fields with these types."
 *
 *   2. Pact starts a mock server that responds exactly as declared — PrescriptionService
 *      never touches the real PatientService during this test.
 *
 *   3. The @Test method calls that mock server, proving PrescriptionService can make
 *      the request correctly and handle the response. If it fails, PrescriptionService
 *      has a bug, not PatientService.
 *
 *   4. Pact writes the verified interaction to target/pacts/PrescriptionService-PatientService.json.
 *      That file is the contract artifact.
 *
 *   5. PatientProviderTest loads that file and replays the interaction against the real
 *      PatientService, verifying PatientService can satisfy what PrescriptionService declared.
 *
 * If PatientService removes the "name" field from its response: PatientProviderTest fails
 * on the provider's PR — before the breaking change is merged. That is the entire guarantee
 * this layer provides.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "PatientService", pactVersion = PactSpecVersion.V3)
class PrescriptionConsumerTest {

    @Pact(consumer = "PrescriptionService")
    public RequestResponsePact patientExistsContract(PactDslWithProvider builder) {
        // PactDslJsonBody declares the MINIMUM fields PrescriptionService depends on.
        // Fields present in the real response but not listed here are irrelevant —
        // the contract is as narrow as the consumer's actual usage. This allows
        // PatientService to add fields or restructure unrelated areas without
        // triggering false contract violations.
        PactDslJsonBody expectedBody = new PactDslJsonBody()
                .integerType("id",    1)
                .stringType("name",   "Leanne Graham")
                .stringType("email",  "Sincere@april.biz")
                .stringType("phone",  "1-770-736-8031 x56442");

        return builder
                .given("patient with ID 1 exists")
                .uponReceiving("a request to retrieve patient record for ID 1")
                    .path("/users/1")
                    .method("GET")
                .willRespondWith()
                    .status(200)
                    .body(expectedBody)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "patientExistsContract")
    void prescriptionService_retrievesPatientRecord_whenPatientExists(MockServer mockServer)
            throws Exception {
        // Call Pact's mock server — not JSONPlaceholder. The mock responds exactly
        // as declared in patientExistsContract, so this test is fully deterministic
        // and requires no network access to the real PatientService.
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServer.getUrl() + "/users/1"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertNotNull(response.body());
    }
}

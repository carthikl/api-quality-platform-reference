package com.wag.qe.api;

import com.wag.qe.api.client.PatientApiClient;
import com.wag.qe.api.client.PrescriptionApiClient;
import com.wag.qe.api.config.ApiConfig;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.testng.Assert.assertTrue;

/**
 * Functional validation for Prescription and Patient APIs.
 * Backed by JSONPlaceholder: /users ≈ patients, /posts ≈ prescriptions.
 */
@Test(groups = "prescription")
public class PrescriptionApiTest {

    private PatientApiClient patientClient;
    private PrescriptionApiClient prescriptionClient;

    @BeforeClass
    public void setup() {
        patientClient = new PatientApiClient();
        prescriptionClient = new PrescriptionApiClient();
        System.out.printf("[REST Assured] Starting %s against: %s%n",
                getClass().getSimpleName(), ApiConfig.baseUrl());
    }

    @Test
    public void getPatientRecord_validId_returns200WithSchema() {
        Response response = patientClient.getPatient(1);

        response.then()
                .statusCode(200)
                .body("id", instanceOf(Integer.class))
                .body("name", notNullValue())
                .body("name", instanceOf(String.class))
                .body("email", matchesPattern("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,6}$"))
                .body("phone", notNullValue())
                .body("phone", instanceOf(String.class))
                .body("address.city", notNullValue())
                .body("address.city", instanceOf(String.class));

        assertTrue(response.getTime() < 2_000,
                String.format("Response time %dms exceeded 2000ms SLA", response.getTime()));
    }

    @Test
    public void getPatientRecord_invalidId_returns404() {
        // Error-path coverage is not optional: consumers depend on 404 semantics to
        // distinguish "patient not found" from a service fault. Without this test,
        // a provider returning 200 with an empty body breaks silent downstream retries.
        Response response = patientClient.getPatient(99999);
        response.then().statusCode(404);
    }

    @Test
    public void submitPrescriptionRefill_validPayload_returns201() {
        Map<String, Object> payload = Map.of(
                "userId", 1,
                "title", "Prescription Refill RX-12345",
                "body", "Refill request for patient 1"
        );

        prescriptionClient.submitPrescriptionRefill(payload)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("id", instanceOf(Integer.class))
                .body("id", greaterThan(0))
                .body("title", equalTo("Prescription Refill RX-12345"))
                .body("userId", equalTo(1));
    }

    @Test
    public void prescriptionSubmission_responseTime_underSLA() {
        // Response time is a distinct concern from functional correctness.
        // A test that validates data accuracy but accepts unlimited latency
        // gives false signal — a degraded service passes all functional tests
        // until a timeout finally surfaces it as a hard failure in production.
        Map<String, Object> payload = Map.of(
                "userId", 1,
                "title", "Prescription Refill RX-12345",
                "body", "Refill request for patient 1"
        );

        Response response = prescriptionClient.submitPrescriptionRefill(payload);

        assertTrue(response.getTime() < 3_000,
                String.format("Prescription submission response time %dms exceeded 3000ms SLA",
                        response.getTime()));
    }
}

package com.wag.qe.api.client;

import com.wag.qe.api.config.ApiConfig;
import io.restassured.response.Response;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * API client for Prescription endpoints.
 * Pure HTTP execution — no assertions, no test logic.
 * Backed by /posts on JSONPlaceholder (posts ≈ prescriptions, userId ≈ patientId).
 */
public class PrescriptionApiClient {

    public Response getPrescriptionsForPatient(int patientId) {
        return given()
                .spec(ApiConfig.getBaseSpec())
                .queryParam("userId", patientId)
                .get("/posts");
    }

    public Response submitPrescriptionRefill(Map<String, Object> payload) {
        return given()
                .spec(ApiConfig.getBaseSpec())
                .body(payload)
                .post("/posts");
    }
}

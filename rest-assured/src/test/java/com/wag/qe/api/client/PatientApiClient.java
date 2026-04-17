package com.wag.qe.api.client;

import com.wag.qe.api.config.ApiConfig;
import io.restassured.response.Response;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * API client for Patient (User) endpoints.
 * Pure HTTP execution — no assertions, no test logic.
 * Backed by /users on JSONPlaceholder for reference implementation.
 */
public class PatientApiClient {

    public Response getPatient(int patientId) {
        return given()
                .spec(ApiConfig.getBaseSpec())
                .pathParam("id", patientId)
                .get("/users/{id}");
    }

    public Response getAllPatients() {
        return given()
                .spec(ApiConfig.getBaseSpec())
                .get("/users");
    }

    public Response createPatient(Map<String, Object> payload) {
        return given()
                .spec(ApiConfig.getBaseSpec())
                .body(payload)
                .post("/users");
    }

    public Response deletePatient(int patientId) {
        return given()
                .spec(ApiConfig.getBaseSpec())
                .pathParam("id", patientId)
                .delete("/users/{id}");
    }
}

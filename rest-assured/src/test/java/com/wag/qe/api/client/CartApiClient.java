package com.wag.qe.api.client;

import com.wag.qe.api.config.ApiConfig;
import io.restassured.response.Response;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * API client for Cart (Todo) endpoints.
 * Pure HTTP execution — no assertions, no test logic.
 * Backed by /todos on JSONPlaceholder (todos ≈ cart items, completed ≈ fulfilled).
 */
public class CartApiClient {

    public Response getCartItemsForPatient(int patientId) {
        return given()
                .spec(ApiConfig.getBaseSpec())
                .queryParam("userId", patientId)
                .get("/todos");
    }

    public Response addCartItem(Map<String, Object> payload) {
        return given()
                .spec(ApiConfig.getBaseSpec())
                .body(payload)
                .post("/todos");
    }

    public Response getAllCartItems() {
        return given()
                .spec(ApiConfig.getBaseSpec())
                .get("/todos");
    }
}

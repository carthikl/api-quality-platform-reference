package com.wag.qe.api;

import com.wag.qe.api.client.CartApiClient;
import com.wag.qe.api.config.ApiConfig;
import io.restassured.http.ContentType;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;

/**
 * Functional validation for Cart API.
 * Backed by /todos on JSONPlaceholder (todos ≈ cart items, completed ≈ fulfilled).
 */
@Test(groups = "cart")
public class CartApiTest {

    private CartApiClient cartClient;

    @BeforeClass
    public void setup() {
        cartClient = new CartApiClient();
        System.out.printf("[REST Assured] Starting %s against: %s%n",
                getClass().getSimpleName(), ApiConfig.baseUrl());
    }

    @Test
    public void getCartItems_validPatient_returnsCollection() {
        cartClient.getCartItemsForPatient(1)
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].userId", equalTo(1))
                .body("[0].id", instanceOf(Integer.class))
                .body("[0].title", notNullValue())
                .body("[0].completed", instanceOf(Boolean.class));
    }

    @Test
    public void addToCart_validItem_returns201() {
        Map<String, Object> payload = Map.of(
                "userId", 1,
                "title", "Metformin 500mg — Qty: 60",
                "completed", false
        );

        cartClient.addCartItem(payload)
                .then()
                .statusCode(201)
                .body("userId", equalTo(1))
                .body("title", equalTo("Metformin 500mg — Qty: 60"))
                .body("completed", equalTo(false))
                .body("id", notNullValue());
    }

    @Test
    public void getCartItems_verifyContentType_isJSON() {
        cartClient.getAllCartItems()
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }
}

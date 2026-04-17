package com.wag.qe.api.config;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static io.restassured.config.RestAssuredConfig.config;

public final class ApiConfig {

    private static final String BASE_URL =
            System.getProperty("api.base.url", "https://jsonplaceholder.typicode.com");

    private ApiConfig() {}

    public static RequestSpecification getBaseSpec() {
        return new RequestSpecBuilder()
                .setBaseUri(BASE_URL)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .setConfig(config()
                        .httpClient(httpClientConfig()
                                // String literals avoid the deprecated CoreConnectionPNames import
                                .setParam("http.connection.timeout", 5_000)
                                .setParam("http.socket.timeout", 10_000)))
                .addFilter(new FailureOnlyLoggingFilter())
                .build();
    }

    public static String baseUrl() {
        return BASE_URL;
    }

    /**
     * Buffers request context and logs full details only on 4xx/5xx responses.
     * Keeps CI output readable on happy-path runs while preserving diagnostics on failures.
     */
    static final class FailureOnlyLoggingFilter implements Filter {

        private static final Logger log = LoggerFactory.getLogger(FailureOnlyLoggingFilter.class);

        @Override
        public Response filter(FilterableRequestSpecification requestSpec,
                               FilterableResponseSpecification responseSpec,
                               FilterContext ctx) {
            Response response = ctx.next(requestSpec, responseSpec);

            if (response.statusCode() >= 400) {
                log.error("API call failed — {} {}{} → HTTP {}",
                        requestSpec.getMethod(),
                        requestSpec.getBaseUri(),
                        requestSpec.getDerivedPath(),
                        response.statusCode());
                log.error("  Request headers:  {}", requestSpec.getHeaders());
                log.error("  Request body:     {}", bodyAsString(requestSpec.getBody()));
                log.error("  Response headers: {}", response.getHeaders());
                log.error("  Response body:    {}", response.getBody().asPrettyString());
            }

            return response;
        }

        private static String bodyAsString(Object body) {
            if (body == null) return "<none>";
            if (body instanceof String s) return s;
            return body.toString();
        }
    }
}

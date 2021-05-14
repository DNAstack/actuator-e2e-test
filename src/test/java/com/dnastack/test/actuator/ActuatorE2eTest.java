package com.dnastack.test.actuator;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

public class ActuatorE2eTest extends BaseE2eTest {

    @Test
    public void appNameAndVersionShouldBeExposed() {
        String actuatorInfoName = requiredEnv("E2E_ACTUATOR_INFO_NAME");

        // @formatter:off
        given()
            .log().method()
            .log().uri()
        .when()
            .get("/actuator/info")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .body("build.name", equalTo(actuatorInfoName))
            .body("build.version", notNullValue());
        // @formatter:on
    }

    @Test
    public void sensitiveInfoShouldNotBeExposed() {
        Stream.of(
            "auditevents",
            "beans",
            "conditions",
            "configprops",
            "env",
            "flyway",
            "httptrace",
            "logfile",
            "loggers",
            "liquibase",
            "metrics",
            "mappings",
            "prometheus",
            "scheduledtasks",
            "sessions",
            "shutdown",
            "threaddump")
        .forEach(endpoint -> {
            given()
                .log().method()
                .log().uri()
            .when()
                .get("/actuator/" + endpoint)
            .then()
                .log().ifValidationFails()
                .statusCode(anyOf(equalTo(401), equalTo(404)));
        });
    }
}

package com.dnastack.test.actuator;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class ActuatorE2eTest {

    protected static ActuatorE2eTestConfig config;

    @BeforeAll
    public static void setup() {
        if (Objects.isNull(config)) {
            config = ActuatorE2eTestConfig.builder().build();
        }
        assertNotNull(config.getBaseUri());
        assertNotNull(config.getActuatorInfoName());

        RestAssured.baseURI = config.getBaseUri();

        if (config.getCookieAuthEnabled() && Objects.isNull(config.getCookies())) {
            config.setCookies(new BasicCookieStore());
        }

        if (config.getAccessTokenAuthEnabled() && Objects.isNull(config.getAccessToken())) {
            config.setAccessToken(getBearerToken(config.getBaseUri()));
        }
    }

    @Test
    public void healthShouldBeExposed() {
        get("/actuator/health")
            .then()
            .body("status", equalTo("UP"));
    }

    @Test
    public void appNameAndVersionShouldBeExposed() {
        String nameKey = "build.name";
        String versionKey = "build.version";
        appNameAndVersionShouldBeExposed(nameKey, versionKey);
    }

    @Test
    public void sensitiveInfoShouldNotBeExposed() {
        Stream.of(getNotExposedActuatorEndpoints())
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

    @Test
    public void sensitiveInfoShouldBeRedirect() {
        assumeTrue(config.getRedirectEnabled());
        Stream.of(getNotExposedActuatorEndpoints())
            .forEach(endpoint -> {
                final Response response = given()
                    .when()
                    .redirects().follow(false)
                    .get("/actuator/" + endpoint)
                    .then()
                    .extract()
                    .response();

                if (response.statusCode() == 302) {
                    assertRedirect(response);
                } else {
                    assertThat(
                        format("Actuator endpoint '%s' is being exposed or misconfigured! Returning status code %s", endpoint, response.getStatusCode()),
                        response.statusCode(), anyOf(equalTo(401), equalTo(404))
                    );
                }
            });
    }

    @Test
    public void sensitiveInfoShouldNotBeExposedWhenLoggedInByCookie() {
        assumeTrue(config.getCookieAuthEnabled());
        Stream.of(getNotExposedActuatorEndpoints())
            .forEach(endpoint -> {
                final Response response = given()
                    .when()
                    .cookies(toRestAssuredCookies(config.getCookies()))
                    .redirects().follow(false)
                    .get("/actuator/" + endpoint)
                    .then()
                    .extract()
                    .response();
                MatcherAssert.assertThat(
                    format("Actuator endpoint '%s' is being exposed or misconfigured! Returning status code %s", endpoint, response.getStatusCode()),
                    response.statusCode(), equalTo(404)
                );
            });
    }

    @Test
    public void sensitiveInfoShouldNotBeExposedWhenLoggedInByAccessToken() {
        assumeTrue(config.getAccessTokenAuthEnabled());
        Stream.of(getNotExposedActuatorEndpoints())
            .forEach(endpoint -> {
                final Response response = given()
                    .when()
                    .auth().oauth2(config.getAccessToken())
                    .redirects().follow(false)
                    .get("/actuator/" + endpoint)
                    .then()
                    .extract()
                    .response();
                assertThat(
                    format("Actuator endpoint '%s' is being exposed or misconfigured! Returning status code %s", endpoint, response.getStatusCode()),
                    response.statusCode(), equalTo(404)
                );
            });
    }

    protected void assertRedirect(Response response) {
        final String locationHeader = response.header("Location");
        assertThat(locationHeader, notNullValue());
        assertThat(locationHeader, equalTo(config.getLoginUrlPath()));
    }

    protected static List<String> getNotExposedActuatorEndpoints() {
        return List.of(
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
            "threaddump");
    }

    protected void appNameAndVersionShouldBeExposed(String nameKey, String versionKey) {
        assumeFalse(localDeploymentUrl().matches(config.getBaseUri()), "Service info isn't set on local dev builds");

        given()
            .log().method()
            .log().uri()
            .when()
            .get("/actuator/info")
            .then()
            .log().ifValidationFails()
            .statusCode(200)
            .body(nameKey, equalTo(config.getActuatorInfoName()))
            .body(versionKey, notNullValue());
    }

    public static String requiredEnv(String name) {
        String val = System.getenv(name);
        if (val == null) {
            fail("Environnment variable `" + name + "` is required");
        }
        return val;
    }

    public static String optionalEnv(String name, String defaultValue) {
        String val = System.getenv(name);
        if (val == null) {
            return defaultValue;
        }
        return val;
    }

    public static String getEnv(String name) {
        return System.getenv(name);
    }

    /**
     * Hamcrest matcher that passes for local deployment URLs and fails for non-local URLs. Useful for assumptions that
     * stop tests that shouldn't be run against local development instances.
     */
    protected static Matcher<String> localDeploymentUrl() {
        return Matchers.anyOf(startsWith("http://localhost"), startsWith("http://host.docker.internal"));
    }

    protected static Cookies toRestAssuredCookies(CookieStore cookieStore) {
        return new Cookies(
            cookieStore.getCookies()
                .stream()
                .map(c -> {
                    final Cookie.Builder builder = new Cookie.Builder(c.getName(), c.getValue());
                    builder.setDomain(c.getDomain());
                    if (c.getExpiryDate() != null) builder.setExpiryDate(c.getExpiryDate());
                    if (c.getExpiryDate() != null) builder.setPath(c.getPath());

                    return builder.build();
                })
                .collect(Collectors.toList())
        );
    }

    protected static String getBearerToken(String requiredResource) {
        Objects.requireNonNull(requiredResource);
        Objects.requireNonNull(config.getWalletUrl());
        Objects.requireNonNull(config.getWalletClientId());
        Objects.requireNonNull(config.getWalletClientSecret());

        RequestSpecification specification = new RequestSpecBuilder()
            .setBaseUri(config.getWalletUrl())
            .build();

        RequestSpecification reqSpec = given(specification)
            .log().uri().auth()
            .basic(config.getWalletClientId(), config.getWalletClientSecret())
            .formParam("grant_type", "client_credentials");

        if (!requiredResource.isBlank()) {
            reqSpec.formParam("resource", requiredResource);
        }

        JsonPath tokenResponse = reqSpec
            .when()
            .post()
            .then()
            .log().ifValidationFails()
            .statusCode(200)
            .extract().jsonPath();

        return tokenResponse.getString("access_token");
    }

}

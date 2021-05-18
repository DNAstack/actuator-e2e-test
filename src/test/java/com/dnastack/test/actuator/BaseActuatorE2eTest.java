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
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisabledIfEnvironmentVariable(named = "E2E_BASE_ACTUATOR_TEST_DISABLED", matches = "true")
public class BaseActuatorE2eTest {

    protected static final String BASE_URI = requiredEnv("E2E_BASE_URI");
    protected static final String ACTUATOR_INFO_NAME = getEnv("E2E_ACTUATOR_INFO_NAME");
    protected static final Boolean LOGIN_REDIRECT_ENABLED = Boolean.parseBoolean(getEnv("E2E_LOGIN_REDIRECT_ENABLED"));
    protected static final String LOGIN_URL_PATH = optionalEnv("E2E_LOGIN_URL_PATH", "/");
    protected static final Boolean COOKIE_AUTH_ENABLED = Boolean.parseBoolean(getEnv("E2E_COOKIE_AUTH_ENABLED"));
    protected static final Boolean ACCESS_TOKEN_AUTH_ENABLED = Boolean.parseBoolean(getEnv("E2E_ACCESS_TOKEN_AUTH_ENABLED"));
    protected static final String WALLET_TOKEN_URI = optionalEnv("E2E_WALLET_TOKEN_URI", "http://localhost:8081/oauth/token");
    protected static final String WALLET_CLIENT_ID = getEnv("E2E_WALLET_CLIENT_ID");
    protected static final String WALLET_CLIENT_SECRET = optionalEnv("E2E_WALLET_CLIENT_SECRET", "dev-secret-never-use-in-prod");

    @BeforeAll
    public static void setup() {
        RestAssured.baseURI = BASE_URI;
    }

    @Test
    public void healthShouldBeExposed() {
        given()
            .log().method()
            .log().uri()
        .when()
            .get("/actuator/health")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .body("status", equalTo("UP"));
    }

    @Test
    public void appNameAndVersionShouldBeExposed() {
        String nameKey = "build.name";
        String versionKey = "build.version";
        appNameAndVersionShouldBeExposed(nameKey, versionKey);
    }

    protected void appNameAndVersionShouldBeExposed(String nameKey, String versionKey) {
        assumeFalse(localDeploymentUrl().matches(BASE_URI), "Service info isn't set on local dev builds");

        JsonPath jsonPath = given()
            .log().method()
            .log().uri()
        .when()
            .get("/actuator/info")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .extract().jsonPath();

        String name = jsonPath.getString(nameKey);
        String version = jsonPath.getString(versionKey);
        if (ACTUATOR_INFO_NAME != null && !ACTUATOR_INFO_NAME.isEmpty()){
            assertThat(name, equalTo(ACTUATOR_INFO_NAME));
        } else {
            assertTrue(name != null && !name.isEmpty());
        }
        assertTrue(version != null && !version.isEmpty());
    }

    @Test
    public void sensitiveInfoShouldNotBeExposed() {
        assumeFalse(LOGIN_REDIRECT_ENABLED);
        getNotExposedActuatorEndpoints()
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
        assumeTrue(LOGIN_REDIRECT_ENABLED);
        getNotExposedActuatorEndpoints()
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

    protected void assertRedirect(Response response) {
        final String locationHeader = response.header("Location");
        assertThat(locationHeader, notNullValue());
        assertThat(locationHeader, equalTo(LOGIN_URL_PATH));
    }

    @Test
    public void sensitiveInfoShouldNotBeExposedWhenLoggedInByCookie() {
        assumeTrue(COOKIE_AUTH_ENABLED);
        CookieStore cookies = getCookie();
        sensitiveInfoShouldNotBeExposedWhenLoggedInByCookie(cookies);
    }

    protected void sensitiveInfoShouldNotBeExposedWhenLoggedInByCookie(CookieStore cookies) {
        getNotExposedActuatorEndpoints()
            .forEach(endpoint -> {
                final Response response = given()
                    .when()
                        .cookies(toRestAssuredCookies(cookies))
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
        assumeTrue(ACCESS_TOKEN_AUTH_ENABLED);
        String accessToken = getAccessToken();
        sensitiveInfoShouldNotBeExposedWhenLoggedInByAccessToken(accessToken);
    }

    protected void sensitiveInfoShouldNotBeExposedWhenLoggedInByAccessToken(String accessToken) {
        getNotExposedActuatorEndpoints()
            .forEach(endpoint -> {
                final Response response = given()
                    .when()
                        .auth().oauth2(accessToken)
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

    protected static Stream<String> getNotExposedActuatorEndpoints() {
        return Stream.of(
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
    protected Matcher<String> localDeploymentUrl() {
        return Matchers.anyOf(startsWith("http://localhost"), startsWith("http://host.docker.internal"));
    }

    protected Cookies toRestAssuredCookies(CookieStore cookieStore) {
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

    protected BasicCookieStore getCookie() {
        return new BasicCookieStore();
    }

    protected String getAccessToken() {
        Objects.requireNonNull(BASE_URI);
        Objects.requireNonNull(WALLET_TOKEN_URI);
        Objects.requireNonNull(WALLET_CLIENT_ID);
        Objects.requireNonNull(WALLET_CLIENT_SECRET);

        RequestSpecification specification = new RequestSpecBuilder()
            .setBaseUri(WALLET_TOKEN_URI)
            .build();

        RequestSpecification reqSpec = given(specification)
            .log().uri().auth()
            .basic(WALLET_CLIENT_ID, WALLET_CLIENT_SECRET)
            .formParam("grant_type", "client_credentials");

        if (!BaseActuatorE2eTest.BASE_URI.isBlank()) {
            reqSpec.formParam("resource", BaseActuatorE2eTest.BASE_URI);
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

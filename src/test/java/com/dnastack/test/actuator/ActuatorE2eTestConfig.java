package com.dnastack.test.actuator;

import lombok.Builder;
import lombok.Data;
import org.apache.http.client.CookieStore;

import static com.dnastack.test.actuator.ActuatorE2eTest.*;

@Data
@Builder
public class ActuatorE2eTestConfig {

    @Builder.Default
    private String baseUri = getEnv("E2E_BASE_URI");
    @Builder.Default
    private String actuatorInfoName = getEnv("E2E_ACTUATOR_INFO_NAME");
    @Builder.Default
    private Boolean accessTokenAuthEnabled = Boolean.parseBoolean(getEnv("E2E_ACCESS_TOKEN_AUTH_ENABLED"));
    @Builder.Default
    private Boolean cookieAuthEnabled = Boolean.parseBoolean(getEnv("E2E_COOKIE_AUTH_ENABLED"));
    @Builder.Default
    private Boolean redirectEnabled = Boolean.parseBoolean(getEnv("E2E_REDIRECT_ENABLED"));
    @Builder.Default
    private String loginUrlPath = optionalEnv("E2E_LOGIN_URL_PATH", "/");
    @Builder.Default
    private String walletUrl = optionalEnv("E2E_WALLET_URL", "http://localhost:8081");
    @Builder.Default
    private String walletClientId = getEnv("E2E_WALLET_CLIENT_ID");
    @Builder.Default
    private String walletClientSecret = optionalEnv("E2E_WALLET_CLIENT_SECRET", "dev-secret-never-use-in-prod");
    private CookieStore cookies;
    private String accessToken;
}

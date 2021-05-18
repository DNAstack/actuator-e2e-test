# actuator-e2e-test
Java library to test /actuator endpoint.

## How to use?
To use this library include `actuator-e2e-test` as dependency and configure it by environmental variables. 
For further customization extend class `ActuatorE2eTest` and override methods you need to customize.

### Add DNAstack Github repository to pom.xml

```
<repositories>
    <repository>
        <id>github</id>
        <name>DNAstack Private Github Packages</name>
        <url>https://maven.pkg.github.com/DNAstack/dnastack-packages</url>
    </repository>
    ...
</repositories>
```

### Add dependency to pom.xml

To use this library in your app include following dependency in your `pom.xml`.

```
<dependency>
  <groupId>com.dnastack</groupId>
  <artifactId>actuator-e2e-test</artifactId>
  <version>${actuator-e2e-test.version}</version>
</dependency>
```

Where `${actuator-e2e-test.version}` is version of the published library. You can find available versions at [github](https://github.com/DNAstack/dnastack-packages/packages/790648).

### Copy maven settings.xml to docker image

Make sure that that maven settings.xml are copied to docker image.

## Configuration

Configuration is done by set of environmental variables

### Required environmental variables

`E2E_BASE_URI` - base URI of running application


### Optional environmental variables

#### Check details

`E2E_ACTUATOR_INFO_NAME` - application name which is should be present in /actuator/info endpoint. Default is notNull validation.

#### Application has login redirect

`E2E_LOGIN_REDIRECT_ENABLED` - set to `true` when unauthenticated requests to /actuator endpoint are redirected to login page.

`E2E_LOGIN_URL_PATH` - login URL path where unauthenticated requests are redirected.
`assertRedirect(Response response)` method can be overridden to customize response validation after redirect.
#### Authentication is done by Cookie

`E2E_COOKIE_AUTH_ENABLED` - set to `true` to validate that sensitive /actuator endpoints are not exposed when user is logged in using Cookie.
Override `getCookie()` method to provide your Cookie.

#### Authentication is done by accessToken

`E2E_ACCESS_TOKEN_AUTH_ENABLED` - set to `true` to validate that sensitive /actuator endpoints are not exposed when user is logged in using accessToken.
Override `getAccessToken()` method to provide your accessToken. AccessToken is by default retrieved using `client_credentials` flow for `E2E_BASE_URI` as resource using these environmental variables:
```
E2E_WALLET_TOKEN_URI
E2E_WALLET_CLIENT_ID
E2E_WALLET_CLIENT_SECRET
```
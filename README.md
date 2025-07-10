# actuator-e2e-test
Java library to test /actuator endpoint.

## How to use?
To use this library include `actuator-e2e-test` as dependency and configure it by environmental variables. 
For further customization extend class `BaseActuatorE2eTest`, override methods you need to customize and set environment property`E2E_BASE_ACTUATOR_TEST_DISABLED` to `true` otherwise tests will be executed from both sub-class and base-class.

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

#### build-docker-e2e-image

```
# Allows us to pass settings.xml configured on local machine or CI server to access private repo
mkdir -p ${docker_context_dir}/.m2
cp ~/.m2/*.xml ${docker_context_dir}/.m2
```

#### Dockerfile

```
# Allows us to pass settings.xml configured on local machine or CI server to access private repo
ADD target/.m2 /root/.m2
```

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

## Deployment Process

This library is published to Maven Central. The deployment process is automated using Concourse CI.

### Build Pipeline

Commits trigger builds in the libraries-build pipeline.

### Release Process

1. **Create a SNAPSHOT tag**: Create an annotated tag on the main branch for the next release with the SNAPSHOT postfix (e.g., `1.2.1-SNAPSHOT`)
   ```bash
   git tag -a 1.2.1-SNAPSHOT -m "Start development of version 1.2.1"
   git push origin 1.2.1-SNAPSHOT
   ```

2. **Development**: Branch, commit, push, and create pull requests as usual

3. **Release**: When the current snapshot is ready to become a release, create the release tag (e.g., `1.2.1`)
   ```bash
   git tag -a 1.2.1 -m "Release version 1.2.1"
   git push origin 1.2.1
   ```

4. **Next development cycle**: Once the release build completes and the new release is available on Maven Central, tag the current commit with the next release version's SNAPSHOT
   ```bash
   git tag -a 1.2.2-SNAPSHOT -m "Start development of version 1.2.2"
   git push origin 1.2.2-SNAPSHOT
   ```

**Note**: Tags must match the regex `^[0-9]+\.[0-9]+\.[0-9]+$` to trigger a release build.
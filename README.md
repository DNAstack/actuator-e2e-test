# actuator-e2e-test
Java library to test /actuator endpoint.

## How to use?

### Add dependency to pom.xml

To use this library in your app include following dependency in your `pom.xml`.

```
<dependency>
  <groupId>com.dnastack</groupId>
  <artifactId>actuator-e2e-test</artifactId>
  <version>${actuator-e2e-test.version}</version>
</dependency>
```

Where `${actuator-e2e-test.version}` is version of the published library. You can find available versions at [nexus.dnastack.com](https://nexus.dnastack.com/content/repositories/releases/com/dnastack/actuator-e2e-test/).

### Add dependency to maven-surefire-plugin configuration

To execute test classes which are not in `testSourceDirectory` dependency needs to be added in `dependenciesToScan` configuration.

```
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <dependenciesToScan>
            <dependency>com.dnastack:actuator-e2e-test</dependency>
        </dependenciesToScan>
        ...
    </configuration>
    ...
</plugin>
```


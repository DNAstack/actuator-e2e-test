# actuator-e2e-test
Java library to test /actuator endpoint.

## How to use?

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

### Add dependency to maven-surefire-plugin configuration

To execute test classes which are not in `testSourceDirectory` dependency needs to be added in `dependenciesToScan` configuration.
When building a docker image, it should be in `ci/e2e-tests/e2e-exec-pom.xml`

```
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <dependenciesToScan>
            <dependency>com.dnastack:actuator-e2e-test</dependency>
            ...
        </dependenciesToScan>
        ...
    </configuration>
    ...
</plugin>
```


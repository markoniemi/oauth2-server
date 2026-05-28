# OAuth2 Server Simplification Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the custom client YAML loading infrastructure with Spring Boot's native `spring.security.oauth2.authorizationserver.client.*` auto-configuration, eliminating the static `ClientConfig` pattern, `ClientProperties`, and helper deserialization classes.

**Architecture:** Convert `clients.yaml` to the native Spring Boot format so the server auto-configures `InMemoryRegisteredClientRepository` without Java code. Update `Container` to generate native-format YAML for programmatic clients instead of using a static field bridge. Inline the `withConfigFile()` user parsing to remove two now-unused helper classes.

**Tech Stack:** Spring Boot 3.5.6, Spring Authorization Server 1.x, Testcontainers, Jackson YAML (already in use)

---

## File Map

| Action | File | Why |
|--------|------|-----|
| Modify | `src/main/resources/application.yaml` | Remove hardcoded users — no silent defaults in Docker image |
| Create | `src/test/resources/application.yaml` | Users for `AuthServerIT` only; not packaged into Docker image |
| Move | `src/main/resources/clients.yaml` → `src/test/resources/clients.yaml` | Convert format + move out of Docker image; test classpath still finds it for `AuthServerIT` |
| Delete | `src/main/java/com/example/auth/config/ClientProperties.java` | No longer needed; native auto-config handles binding |
| Modify | `src/main/java/com/example/auth/AuthServerApplication.java` | Remove `ClientProperties` from `@EnableConfigurationProperties` |
| Delete | `src/main/java/com/example/auth/testcontainers/ClientConfig.java` | Spring Boot auto-configures `RegisteredClientRepository`; static bridge gone |
| Delete | `src/test/java/com/example/auth/testcontainers/ClientConfigTest.java` | Tests deleted class |
| Modify | `src/main/java/com/example/auth/testcontainers/Container.java` | Generate native client YAML; inline `withConfigFile` parsing; remove `registerClients`/`stop` override |
| Delete | `src/main/resources/application-testcontainers.yaml` | `allow-bean-definition-overriding` only needed for the now-deleted `@Primary` ClientConfig bean |
| Delete | `src/main/java/com/example/auth/testcontainers/UserConfig.java` | Replaced by inline Map parsing in `withConfigFile` |
| Delete | `src/main/java/com/example/auth/testcontainers/SecurityConfig.java` | Same — only existed to support UserConfig deserialization |

---

### Task 1: Convert and move `clients.yaml` to test resources

Spring Boot 3.1+ auto-configures `InMemoryRegisteredClientRepository` from
`spring.security.oauth2.authorizationserver.client.*` with no Java code. The current
`app.oauth2.clients` format is a custom invention; switch to the standard. Then move the file to
`src/test/resources` so the Docker image ships with no default clients — testcontainers callers
must use `withOAuth2Client()` explicitly. `AuthServerIT` still finds the file because
`src/test/resources` is on the test classpath. The `optional:` import prefix means the server
won't fail if the file is absent at runtime.

**Files:**
- Move: `src/main/resources/clients.yaml` → `src/test/resources/clients.yaml`
- Delete: `src/main/java/com/example/auth/config/ClientProperties.java`
- Modify: `src/main/java/com/example/auth/AuthServerApplication.java`
- Modify: `src/main/java/com/example/auth/testcontainers/ClientConfig.java`

- [ ] **Step 1: Write `src/test/resources/clients.yaml` with native format**

Create the file at the new location with converted content:

```yaml
spring:
  security:
    oauth2:
      authorizationserver:
        client:
          frontend-client:
            registration:
              client-id: frontend-client
              client-authentication-methods:
                - none
              authorization-grant-types:
                - authorization_code
              redirect-uris:
                - http://localhost:8080
                - http://localhost:5173
              post-logout-redirect-uris:
                - http://localhost:5173
                - http://localhost:8080
              scopes:
                - openid
                - profile
                - email
            require-proof-key: true
            token:
              access-token-time-to-live: 1h
              refresh-token-time-to-live: 7d
```

- [ ] **Step 2: Delete `src/main/resources/clients.yaml`**

```
git rm src/main/resources/clients.yaml
```

The `spring.config.import` line in `application.yaml` uses `optional:` so the server starts fine
without it. No change needed to `application.yaml`.

- [ ] **Step 3: Remove `ClientProperties` from `AuthServerApplication`**

`src/main/java/com/example/auth/AuthServerApplication.java`:

```java
package com.example.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.example.auth.config.SecurityProperties;

@SpringBootApplication
@EnableConfigurationProperties(SecurityProperties.class)
public class AuthServerApplication {
  public static void main(String[] args) {
    SpringApplication.run(AuthServerApplication.class, args);
  }
}
```

- [ ] **Step 4: Remove `createFromProperties` path from `ClientConfig`**

We keep `ClientConfig` for now (it still serves the testcontainers clients path). Remove the
`createFromProperties` branch, both helper methods, and the `ClientProperties` parameter.

`src/main/java/com/example/auth/testcontainers/ClientConfig.java` — replace entire file:

```java
package com.example.auth.testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnNotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

@Configuration
public class ClientConfig {

    private static List<Client> clients = null;

    public static void setClients(List<Client> clients) {
        ClientConfig.clients = new ArrayList<>(clients);
    }

    public static void clearClients() {
        ClientConfig.clients = null;
    }

    @Bean
    @Primary
    public RegisteredClientRepository testcontainersRegisteredClientRepository() {
        if (clients == null) {
            return new InMemoryRegisteredClientRepository();
        }
        List<RegisteredClient> registeredClients = new ArrayList<>();
        for (Client client : clients) {
            RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(client.getClientId())
                .clientSecret(client.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);

            for (String uri : client.getRedirectUris()) {
                builder.redirectUri(uri);
            }
            for (String scope : client.getScopes()) {
                builder.scope(scope);
            }
            for (String grantType : client.getGrantTypes()) {
                if ("authorization_code".equals(grantType)) {
                    builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
                } else if ("refresh_token".equals(grantType)) {
                    builder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
                } else if ("client_credentials".equals(grantType)) {
                    builder.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);
                }
            }
            registeredClients.add(builder.build());
        }
        return new InMemoryRegisteredClientRepository(registeredClients);
    }
}
```

- [ ] **Step 5: Delete `ClientProperties.java`**

Delete file: `src/main/java/com/example/auth/config/ClientProperties.java`

- [ ] **Step 6: Run `AuthServerIT` to verify native client format works**

```
mvn test -pl . -Dtest=AuthServerIT -Dit.test=AuthServerIT
```

Expected: all tests in `AuthServerIT` pass. If they fail with "client not found", the YAML format
is wrong — double-check property names against the Spring Boot reference for your version.

- [ ] **Step 7: Commit**

```
git add src/test/resources/clients.yaml src/main/java/com/example/auth/AuthServerApplication.java src/main/java/com/example/auth/testcontainers/ClientConfig.java
git rm src/main/resources/clients.yaml src/main/java/com/example/auth/config/ClientProperties.java
git commit -m "Use native Spring Boot client YAML format; move clients.yaml to test resources; remove ClientProperties"
```

---

### Task 2: Delete `ClientConfig` — generate native client YAML in `Container`

Instead of a static `List<Client>` bridge from `Container` to `ClientConfig`, generate the native
`spring.security.oauth2.authorizationserver.client.*` YAML directly in the mounted config file.
Spring Boot reads it at startup; no Java registration code needed.

**Files:**
- Modify: `src/main/java/com/example/auth/testcontainers/Container.java`
- Delete: `src/main/java/com/example/auth/testcontainers/ClientConfig.java`
- Delete: `src/test/java/com/example/auth/testcontainers/ClientConfigTest.java`

- [ ] **Step 1: Rewrite `Container.java`**

Replace the entire file content:

```java
package com.example.auth.testcontainers;

import static java.util.Arrays.asList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class Container extends GenericContainer<Container> {

    private static final int AUTH_SERVER_PORT = 9000;
    private static final String IMAGE_NAME = "ghcr.io/markoniemi/oauth2-server:latest";

    private final List<User> users = new ArrayList<>();
    private final List<Client> clients = new ArrayList<>();
    private String issuerUrl;

    public Container() {
        super(DockerImageName.parse(IMAGE_NAME));
        withExposedPorts(AUTH_SERVER_PORT);
        waitingFor(Wait.forHttp("/actuator/health")
            .forStatusCode(200)
            .withStartupTimeout(Duration.ofMinutes(2)));
    }

    public Container withUser(String username, String password, String... roles) {
        users.add(new User(username, password, new HashSet<>(asList(roles))));
        return this;
    }

    public Container withOAuth2Client(Client client) {
        clients.add(client);
        return this;
    }

    public Container withIssuerUrl(String issuerUrl) {
        this.issuerUrl = issuerUrl;
        return this;
    }

    public Container withConfigFile(String filePath) {
        try {
            String resolvedPath = filePath;
            if (filePath.startsWith("classpath:")) {
                resolvedPath = filePath.substring("classpath:".length());
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                var resource = classLoader.getResource(resolvedPath);
                if (resource == null) {
                    throw new IllegalArgumentException("Classpath resource not found: " + resolvedPath);
                }
                resolvedPath = resource.getPath();
            }

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            @SuppressWarnings("unchecked")
            Map<String, Object> yaml = mapper.readValue(new File(resolvedPath), Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> app = (Map<String, Object>) yaml.get("app");
            if (app != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> security = (Map<String, Object>) app.get("security");
                if (security != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> usersList = (List<Map<String, Object>>) security.get("users");
                    if (usersList != null) {
                        for (Map<String, Object> u : usersList) {
                            String username = (String) u.get("username");
                            String password = (String) u.get("password");
                            @SuppressWarnings("unchecked")
                            List<String> roles = (List<String>) u.get("roles");
                            if (username != null && password != null && roles != null && !roles.isEmpty()) {
                                users.add(new User(username, password, new HashSet<>(roles)));
                            }
                        }
                    }
                }
            }
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config file: " + filePath, e);
        }
    }

    public String getAuthServerUrl() {
        return "http://localhost:" + getMappedPort(AUTH_SERVER_PORT);
    }

    public String getIssuerUrl() {
        if (issuerUrl != null) {
            return issuerUrl;
        }
        return getAuthServerUrl();
    }

    @Override
    protected void configure() {
        try {
            generateAndMountConfigYaml();
        } catch (IOException e) {
            throw new RuntimeException("Failed to configure container", e);
        }
    }

    private void generateAndMountConfigYaml() throws IOException {
        if (users.isEmpty() && clients.isEmpty()) {
            return;
        }

        Map<String, Object> root = new LinkedHashMap<>();

        if (!users.isEmpty()) {
            List<Map<String, Object>> usersList = new ArrayList<>();
            for (User user : users) {
                Map<String, Object> userMap = new LinkedHashMap<>();
                userMap.put("username", user.getUsername());
                userMap.put("password", user.getPassword());
                userMap.put("roles", new ArrayList<>(user.getRoles()));
                usersList.add(userMap);
            }
            Map<String, Object> securityMap = new LinkedHashMap<>();
            securityMap.put("users", usersList);
            Map<String, Object> appMap = new LinkedHashMap<>();
            appMap.put("security", securityMap);
            root.put("app", appMap);
        }

        if (!clients.isEmpty()) {
            Map<String, Object> clientsMap = new LinkedHashMap<>();
            for (Client client : clients) {
                Map<String, Object> registration = new LinkedHashMap<>();
                registration.put("client-id", client.getClientId());
                registration.put("client-secret", client.getClientSecret());
                registration.put("client-authentication-methods", List.of("client_secret_basic"));
                registration.put("authorization-grant-types", new ArrayList<>(client.getGrantTypes()));
                registration.put("redirect-uris", new ArrayList<>(client.getRedirectUris()));
                registration.put("scopes", new ArrayList<>(client.getScopes()));

                Map<String, Object> clientEntry = new LinkedHashMap<>();
                clientEntry.put("registration", registration);
                clientsMap.put(client.getClientId(), clientEntry);
            }

            Map<String, Object> authserverMap = new LinkedHashMap<>();
            authserverMap.put("client", clientsMap);
            Map<String, Object> oauth2Map = new LinkedHashMap<>();
            oauth2Map.put("authorizationserver", authserverMap);
            Map<String, Object> secMap = new LinkedHashMap<>();
            secMap.put("oauth2", oauth2Map);
            Map<String, Object> springMap = new LinkedHashMap<>();
            springMap.put("security", secMap);
            root.put("spring", springMap);
        }

        File tempDir = Files.createTempDirectory("oauth2-config-").toFile();
        File configFile = new File(tempDir, "application.yaml");
        new ObjectMapper(new YAMLFactory()).writeValue(configFile, root);

        withCopyFileToContainer(
            MountableFile.forHostPath(configFile.getAbsolutePath()),
            "/config/application.yaml");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.delete(configFile.toPath());
                Files.delete(tempDir.toPath());
            } catch (IOException e) {
                // Ignore cleanup errors
            }
        }));
    }
}
```

Key changes from original:
- Removed `withEnv("SPRING_PROFILES_ACTIVE", "testcontainers")` from constructor
- Removed `registerClients()` method
- Removed `stop()` override (no longer calls `ClientConfig.clearClients()`)
- `generateAndMountUsersYaml()` → `generateAndMountConfigYaml()`: generates both users and clients; clients use native `spring.security.oauth2.authorizationserver.client.*` format
- `withConfigFile()` parsing is now inlined (no `UserConfig`/`SecurityConfig` helper classes)

- [ ] **Step 2: Delete `ClientConfig.java`**

Delete file: `src/main/java/com/example/auth/testcontainers/ClientConfig.java`

- [ ] **Step 3: Delete `ClientConfigTest.java`**

Delete file: `src/test/java/com/example/auth/testcontainers/ClientConfigTest.java`

- [ ] **Step 4: Run unit tests and `AuthServerIT` (no Docker)**

```
mvn test -pl .
```

Expected: all unit tests and `AuthServerIT` pass. Catches compile errors and Spring wiring breaks
before spending time on Docker startup.

- [ ] **Step 5: Run `ContainerClientsIT` to verify native client YAML generation (Docker)**

```
mvn verify -pl . -Dit.test=ContainerClientsIT -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: all three tests pass (`containerIsRunningWithClient`, `authorizationEndpointIsAccessible`,
`clientCanObtainTokenViaClientCredentials`). If `containerIsRunningWithClient` fails, the generated
YAML is malformed — inspect `/config/application.yaml` by adding a debug print before `writeValue`.

- [ ] **Step 6: Commit**

```
git rm src/main/java/com/example/auth/testcontainers/ClientConfig.java
git rm src/test/java/com/example/auth/testcontainers/ClientConfigTest.java
git add src/main/java/com/example/auth/testcontainers/Container.java
git commit -m "Generate native client YAML in Container; remove ClientConfig static bridge"
```

---

### Task 3: Delete `application-testcontainers.yaml`

`allow-bean-definition-overriding: true` was required because `ClientConfig` declared a `@Primary`
`RegisteredClientRepository` that conflicted with Spring Boot's auto-configured one. Both are now
gone. The profile file and its activation are dead weight.

**Files:**
- Delete: `src/main/resources/application-testcontainers.yaml`

The `withEnv("SPRING_PROFILES_ACTIVE", "testcontainers")` line was already removed from
`Container.java` in Task 2, Step 1.

- [ ] **Step 1: Delete `application-testcontainers.yaml`**

Delete file: `src/main/resources/application-testcontainers.yaml`

- [ ] **Step 2: Run unit tests and `AuthServerIT` (no Docker)**

```
mvn test -pl .
```

Expected: all pass. A failure here means `allow-bean-definition-overriding` was load-bearing for
something other than `ClientConfig` — check which bean conflicts in the output.

- [ ] **Step 3: Run full integration test suite (Docker)**

```
mvn verify -pl .
```

Expected: all tests pass. A failure here likely means something else was relying on the profile.
Check `mvn verify` output for which test failed and what bean/property it was missing.

- [ ] **Step 4: Commit**

```
git rm src/main/resources/application-testcontainers.yaml
git commit -m "Remove testcontainers profile; no longer needed after ClientConfig deletion"
```

---

### Task 4: Inline `withConfigFile()` parsing — delete `UserConfig` and `testcontainers/SecurityConfig`

`UserConfig` and `testcontainers/SecurityConfig` exist only as Jackson deserialization targets for
`withConfigFile()`. The new `Container.java` from Task 2 already inlines that parsing as raw
`Map` traversal. These two files are now unreachable.

**Files:**
- Delete: `src/main/java/com/example/auth/testcontainers/UserConfig.java`
- Delete: `src/main/java/com/example/auth/testcontainers/SecurityConfig.java`

> **Note:** `Container.java` was already rewritten in Task 2 to inline the parsing. Verify no
> import of `UserConfig` or `testcontainers.SecurityConfig` remains in Container.java before
> deleting.

- [ ] **Step 1: Confirm no remaining imports**

Run:
```
grep -r "UserConfig\|testcontainers\.SecurityConfig" src/main/java
```

Expected: no matches. If matches appear, remove those imports before deleting the files.

- [ ] **Step 2: Delete the helper classes**

```
git rm src/main/java/com/example/auth/testcontainers/UserConfig.java
git rm src/main/java/com/example/auth/testcontainers/SecurityConfig.java
```

- [ ] **Step 3: Run unit tests and `AuthServerIT` (no Docker)**

```
mvn test -pl .
```

Expected: all pass. Confirms the deleted classes had no remaining callers.

- [ ] **Step 4: Run `ContainerConfigFileIT` (Docker)**

```
mvn verify -pl . -Dit.test=ContainerConfigFileIT -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: all tests pass including `loadsUsersFromYamlFile`, `mixesFileAndFluentConfig`, and
`throwsOnMissingFile`. The `throwsOnMissingFile` test verifies that the classpath-not-found path
still throws `RuntimeException` — confirm this still happens in the inlined code.

- [ ] **Step 5: Run full test suite one final time (Docker)**

```
mvn verify -pl .
```

Expected: all tests pass, build is green.

- [ ] **Step 6: Commit**

```
git commit -m "Delete UserConfig and SecurityConfig helpers; withConfigFile uses inline Map parsing"
```

---

### Task 5: Split `application.yaml` — remove default users from Docker image

Default users in `src/main/resources/application.yaml` are silently packaged into the Docker image.
Any testcontainers test that forgets `withUser()` still works, masking a misconfiguration. Move
users to `src/test/resources/application.yaml` so they are available to `AuthServerIT` (test
classpath) but absent from the Docker image. Testcontainers ITs must explicitly provide users via
`withUser()` or `withConfigFile()`.

Maven classpath note: during `mvn test`/`mvn verify`, `src/test/resources` takes precedence over
`src/main/resources`. Spring Boot therefore picks up `src/test/resources/application.yaml` for
in-process tests, but the Docker image is built only from `src/main/resources`.

**Files:**
- Modify: `src/main/resources/application.yaml`
- Create: `src/test/resources/application.yaml`

- [ ] **Step 1: Strip users from `src/main/resources/application.yaml`**

```yaml
spring:
  application:
    name: auth-server
  config:
    import: optional:classpath:/config/application.yaml,optional:classpath:/clients.yaml
  security:
    oauth2:
      authorizationserver:
        issuer: http://localhost:9000

server:
  port: 9000
  servlet:
    context-path: /
```

- [ ] **Step 2: Create `src/test/resources/application.yaml` with users for `AuthServerIT`**

```yaml
app:
  security:
    users:
      - username: admin
        password: admin
        roles:
          - USER
          - ADMIN
      - username: user
        password: user
        roles:
          - USER
```

- [ ] **Step 3: Run `AuthServerIT` to confirm it still finds users**

```
mvn test -pl . -Dtest=AuthServerIT
```

Expected: all tests pass. If it fails with "No UserDetailsService bean" or authentication errors,
Spring Boot is not picking up `src/test/resources/application.yaml` — verify the file path is
exactly `src/test/resources/application.yaml` (not a subdirectory).

- [ ] **Step 4: Run a testcontainers IT that requires a user to confirm Docker image has no defaults**

```
mvn verify -pl . -Dit.test=ContainerAuthFlowIT -Dsurefire.failIfNoSpecifiedTests=false
```

This test should already call `withUser()`. If it passes, the container is getting users only from
the generated config file, not from baked-in defaults.

- [ ] **Step 5: Commit**

```
git add src/main/resources/application.yaml src/test/resources/application.yaml
git commit -m "Move default users to test resources; Docker image has no hardcoded users"
```

---

## Summary of changes

| File | Action | Lines |
|------|--------|-------|
| `config/ClientProperties.java` | Delete | 29 |
| `testcontainers/ClientConfig.java` | Delete | 142 |
| `testcontainers/UserConfig.java` | Delete | 17 |
| `testcontainers/SecurityConfig.java` | Delete | 10 |
| `application-testcontainers.yaml` | Delete | 3 |
| `ClientConfigTest.java` | Delete | 47 |
| `src/main/resources/application.yaml` | Modify (remove users) | -12 |
| `src/test/resources/application.yaml` | Create (users only) | +13 |
| `src/main/resources/clients.yaml` | Move to test resources | 0 |
| **Net deleted** | | **~248 lines** |

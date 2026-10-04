# OAuth2 TestContainers Integration

OAuth2TestContainers is a reusable library that allows you to spin up an OAuth2 Authorization Server in Docker during testing, enabling integration tests for applications that depend on OAuth2 authentication.

## Overview

This library provides a TestContainers extension that:
- Manages the lifecycle of an OAuth2 Authorization Server Docker container (Spring Boot 4.0.3, Spring Security 7.0)
- Starts with the server's bundled defaults: a public PKCE client `frontend-client` and no users
- Configures users and clients with a plain Spring Boot config file that overrides the defaults
- Supports custom issuer URLs and context paths

## Dependencies

The library is published as its own artifact; it brings only Testcontainers with it:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>auth-server-testcontainers</artifactId>
    <version>0.1-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

The server itself runs from the Docker image `ghcr.io/markoniemi/oauth2-server`; the library does not depend on server code.

## Basic Usage

```java
@BeforeAll
static void setUp() {
    container = new OAuth2Container()
        .withConfigFile("oauth2-server.yaml");  // classpath resource, e.g. src/test/resources
    container.start();
}

@AfterAll
static void tearDown() {
    if (container != null) {
        container.stop();
    }
}
```

The file is mounted as the server's `config/application.yaml`. It uses the server's own Spring Boot properties:

- `app.security.users` — login users (password in plain text or with an encoding id, e.g. `{bcrypt}...`)
- `app.cors.allowed-origins` — browser origins allowed to call the server
- `spring.security.oauth2.authorizationserver.client.<key>` — client registrations

## Bundled Defaults

Without a config file the server starts with its bundled `application.yaml`:

- A public `frontend-client`: auth method `none`, `authorization_code` only, PKCE required, scopes `openid profile email`, a 1h access token, and redirect and post-logout URIs for `http://localhost:8080` and `http://localhost:5173`
- `app.cors.allowed-origins` for the same two origins
- No users

## How the Config File Overrides the Defaults

The mounted file is read after the bundled one, so its values win:

| Kind of value | Behavior | Example |
|---------------|----------|---------|
| Maps | Merged by key | A client under `client.<key>` is added, or overrides single fields of an existing client with the same key |
| Single values | Replaced | `require-proof-key`, `access-token-time-to-live` |
| Lists | Replaced whole, never merged | `app.security.users`, `app.cors.allowed-origins`, `redirect-uris` |

Consequences:

- A client under a new key gets no defaults: give it a full registration (auth methods, grant types, scopes).
- A list in the config file must hold every entry, not only the additions.
- `frontend-client` is always registered; a config file cannot remove it.
- A broken config file shows up as the server refusing to start, i.e. a startup timeout. Check the container logs (`withLogConsumer`).

## Config File Examples

### Users

```yaml
app:
  security:
    users:
      - { username: admin, password: admin, roles: [USER, ADMIN] }
      - { username: user, password: user, roles: [USER] }
```

### Public Client (PKCE, no secret)

```yaml
app:
  cors:
    allowed-origins: [http://localhost:5173]
spring:
  security:
    oauth2:
      authorizationserver:
        client:
          spa-client:
            registration:
              client-id: spa-client
              client-authentication-methods: [none]
              authorization-grant-types: [authorization_code]
              redirect-uris: [http://localhost:5173]
              post-logout-redirect-uris: [http://localhost:5173]
              scopes: [openid, profile, email]
            require-proof-key: true
            token:
              access-token-time-to-live: 1h   # Spring default is 5 minutes
```

The server never issues refresh tokens to public clients, so don't list `refresh_token`.

### Confidential Client (with secret)

```yaml
spring:
  security:
    oauth2:
      authorizationserver:
        client:
          backend-client:
            registration:
              client-id: backend-client
              client-secret: backend-secret        # or encoded, e.g. "{bcrypt}..."
              client-authentication-methods: [client_secret_basic]   # or client_secret_post
              authorization-grant-types: [authorization_code, refresh_token, client_credentials]
              redirect-uris: [http://localhost:8080/callback]
              scopes: [openid, profile, api]
            require-proof-key: false   # Spring Authorization Server otherwise requires PKCE for every client
```

### Custom Issuer URL and Context Path

These are set through environment variables, so they work with or without a config file:

```java
container = new OAuth2Container()
    .withConfigFile("oauth2-server.yaml")
    .withIssuerUrl("https://auth.example.com")
    .withContextPath("/auth");
container.start();

String url = container.getAuthServerUrl();  // http://localhost:randomPort/auth
String issuer = container.getIssuerUrl();   // https://auth.example.com
```

## API Reference

### OAuth2Container

**Constructors:**

- `new OAuth2Container()` - Uses `ghcr.io/markoniemi/oauth2-server:latest`
- `new OAuth2Container(DockerImageName image)` - Uses the given image, e.g. a pinned tag

**Methods:**

- `withConfigFile(String configResourcePath)` - Mount a classpath resource as the server's `config/application.yaml`, overriding the bundled defaults
- `withIssuerUrl(String issuerUrl)` - Fix the issuer (`iss` claim, discovery). Default: derived from the request URL, i.e. equals `getAuthServerUrl()`
- `withContextPath(String contextPath)` - Serve under a servlet context path (included in `getAuthServerUrl()`)
- `getAuthServerUrl()` - Get the server URL (http://localhost:mappedPort)
- `getIssuerUrl()` - Get the issuer URL (custom if set, otherwise auth server URL)

All `GenericContainer` methods (`start()`, `stop()`, `withLogConsumer(...)`, ...) are available too.

## Complete Example

`src/test/resources/oauth2-server.yaml`:

```yaml
app:
  security:
    users:
      - { username: user, password: password, roles: [USER] }
spring:
  security:
    oauth2:
      authorizationserver:
        client:
          backend-service:
            registration:
              client-id: backend-service
              client-secret: backend-secret
              client-authentication-methods: [client_secret_basic]
              authorization-grant-types: [client_credentials]
              scopes: [api]
```

```java
public class OAuth2AuthenticationIT {

    private static OAuth2Container container;
    private RestClient restClient = RestClient.create();

    @BeforeAll
    static void setUp() {
        container = new OAuth2Container().withConfigFile("oauth2-server.yaml");
        container.start();
    }

    @AfterAll
    static void tearDown() {
        if (container != null) {
            container.stop();
        }
    }

    @Test
    void defaultFrontendClientRedirectsToLogin() throws Exception {
        WebClient webClient = new WebClient();
        String authUrl = container.getAuthServerUrl() + "/oauth2/authorize?" +
            "response_type=code&" +
            "client_id=frontend-client&" +
            "redirect_uri=http://localhost:5173&" +
            "scope=openid&" +
            "code_challenge=E9Mrozoa2owUednRPg8w_-dvznju3T92jVWswbCQQWE&" +
            "code_challenge_method=S256";

        var page = webClient.getPage(authUrl);

        assertTrue(page.getUrl().toString().contains("/login"));
    }

    @Test
    void confidentialClientGetsToken() {
        var response = restClient.post()
            .uri(container.getAuthServerUrl() + "/oauth2/token")
            .header("Authorization", "Basic " + Base64.getEncoder()
                .encodeToString("backend-service:backend-secret".getBytes()))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body("grant_type=client_credentials&scope=api")
            .retrieve()
            .toEntity(String.class);

        assertEquals(200, response.getStatusCode().value());
    }
}
```

## Troubleshooting

### Container fails to start

Ensure Docker is running and accessible. TestContainers will automatically detect your Docker environment (Npipe on Windows, Unix socket on Linux/macOS).

Verify OAuth2 server image exists: `docker images | grep oauth2-server`

If the container times out, the server most likely rejected the config file (e.g. a client without grant types, or `authorization_code` without redirect URIs). Attach a log consumer to see why:

```java
.withLogConsumer(new Slf4jLogConsumer(log))
```

### Ports in use

TestContainers automatically selects available ports. If you get port errors, ensure you're not hardcoding ports in your tests - use `container.getAuthServerUrl()` instead.

### PKCE validation failures

If you see "code_challenge" parameter errors, either send PKCE parameters:
```java
"code_challenge=" + codeChallenge + "&code_challenge_method=S256"
```
or, for a confidential client, set `require-proof-key: false` in its registration.

### Slow test execution

Starting a Docker container takes time. Consider:
- Using container.start() in @BeforeAll (shared across tests) rather than @BeforeEach
- Running integration tests separately from unit tests
- Using container reuse (see TestContainers documentation)

## Integration with Spring Boot Applications

For testing Spring Boot applications that depend on OAuth2:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MyApplicationIT {

    private static OAuth2Container oauth2;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
            oauth2::getIssuerUrl);
    }

    @BeforeAll
    static void setup() {
        oauth2 = new OAuth2Container().withConfigFile("oauth2-server.yaml");
        oauth2.start();
    }

    @AfterAll
    static void cleanup() {
        if (oauth2 != null) {
            oauth2.stop();
        }
    }
}
```

## Version Compatibility

| OAuth2 Server Version | Spring Boot | Spring Security | Status |
|---|---|---|---|
| 4.0.3+ | 4.0.3 | 7.0+ | Current |
| 0.1-SNAPSHOT | 3.5.6 | 6.x | Deprecated |

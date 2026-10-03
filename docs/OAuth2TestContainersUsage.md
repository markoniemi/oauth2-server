# OAuth2 TestContainers Integration

OAuth2TestContainers is a reusable library that allows you to spin up an OAuth2 Authorization Server in Docker during testing, enabling integration tests for applications that depend on OAuth2 authentication.

## Overview

This library provides a TestContainers extension that:
- Manages the lifecycle of an OAuth2 Authorization Server Docker container (Spring Boot 4.0.3, Spring Security 7.0)
- Configures users and roles for authentication testing
- Registers OAuth2 clients programmatically (supports public clients and confidential clients)
- Supports PKCE (Proof Key for Code Exchange) for public clients
- Supports custom token endpoint auth methods (client_secret_basic, client_secret_post, none)
- Supports custom issuer URLs and context paths
- Provides fluent builder API for easy setup

## Dependencies

TestContainers integration is included with the OAuth2 Authorization Server library. Downstream projects should add:

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.20.3</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-yaml</artifactId>
    <version>2.18.1</version>
    <scope>test</scope>
</dependency>
```

**Note:** OAuth2 Authorization Server upgraded to Spring Boot 4.0.3 and Spring Security 7.0. All clients must support PKCE for public client flows.

## Basic Usage

### Starting a Container with Users

```java
@BeforeAll
static void setUp() {
    container = new OAuth2Container()
        .withUser("testuser", "password", "USER")
        .withUser("admin", "password", "ADMIN", "USER");
    container.start();
}

@AfterAll
static void tearDown() {
    if (container != null) {
        container.stop();
    }
}
```

### Registering OAuth2 Clients

**Confidential Client (with secret):**

```java
container = new OAuth2Container()
    .withOAuth2Client(
        new Client("client-id", "client-secret")
            .withRedirectUris("http://localhost:8080/callback")
            .withScopes("openid", "profile", "email")
    );
container.start();
```

**Public Client (PKCE, no secret):**

```java
container = new OAuth2Container()
    .withOAuth2Client(
        new Client("frontend-client", "")  // Empty secret for public client
            .withRedirectUris("http://localhost:8080", "http://localhost:5173")
            .withPostLogoutRedirectUris("http://localhost:8080", "http://localhost:5173")
            .withScopes("openid", "profile", "email")
            .withAccessTokenTimeToLive(Duration.ofHours(1))  // Spring default is 5 minutes
            // Public clients always get auth method "none" and PKCE; refresh_token grant is dropped
    );
container.start();
```

**Custom Token Endpoint Auth Method:**

```java
container = new OAuth2Container()
    .withOAuth2Client(
        new Client("backend-client", "client-secret")
            .withRedirectUris("http://localhost:8080/callback")
            .withScopes("api")
            .withTokenEndpointAuthMethod("client_secret_post")  // or "client_secret_basic" (default), "none"
    );
container.start();
```


### Custom Issuer URL and Context Path

```java
container = new OAuth2Container()
    .withUser("testuser", "testpass", "USER")
    .withIssuerUrl("https://auth.example.com")
    .withContextPath("/auth");
container.start();

String url = container.getAuthServerUrl();  // http://localhost:randomPort
String issuer = container.getIssuerUrl();   // https://auth.example.com
```

## API Reference

### OAuth2Container

Main entry point for the TestContainers integration.

**Constructors:**

- `new OAuth2Container()` - Uses `ghcr.io/markoniemi/oauth2-server:latest`
- `new OAuth2Container(DockerImageName image)` - Uses the given image, e.g. a pinned tag

**Methods:**

- `withUser(String username, String password, String... roles)` - Add a user with roles
- `withOAuth2Client(Client client)` - Register an OAuth2 client
- `withIssuerUrl(String issuerUrl)` - Set custom issuer URL
- `withContextPath(String contextPath)` - Set custom context path
- `getAuthServerUrl()` - Get the server URL (http://localhost:mappedPort)
- `getIssuerUrl()` - Get the issuer URL (custom if set, otherwise auth server URL)
- `start()` - Start the container
- `stop()` - Stop the container
- `isRunning()` - Check if container is running

### Client

Represents an OAuth2 client registration.

**Constructor:**
```java
new Client(String clientId, String clientSecret)
```

**Note:** Pass empty string `""` for public clients (PKCE flow).

**Methods:**

- `withRedirectUris(String... uris)` - Add redirect URIs (supports multiple)
- `withScopes(String... scopes)` - Add allowed scopes
- `withGrantTypes(String... grantTypes)` - Set allowed grant types (default: authorization_code, refresh_token)
- `withTokenEndpointAuthMethod(String method)` - Set token endpoint auth method:
  - `"client_secret_basic"` (default for clients with secrets)
  - `"client_secret_post"` 
  - `"none"` (default for public clients)
- `withRequireProofKey(boolean)` - Require PKCE for a confidential client (default `false`; always on for public clients). Written explicitly because Spring Authorization Server otherwise requires PKCE for every client
- `withPostLogoutRedirectUris(String... uris)` - Allowed `post_logout_redirect_uri` values for OIDC logout
- `withAccessTokenTimeToLive(Duration ttl)` - Access token lifetime (Spring default: 5 minutes)
- `withRefreshTokenTimeToLive(Duration ttl)` - Refresh token lifetime (Spring default: 60 minutes)

Clients are passed to the server as Spring Boot `spring.security.oauth2.authorizationserver.client.<client-id>.*` properties.

### User

Represents a user (internal - created via `withUser()`).

Users include username, password, and a set of roles.

## Complete Example

```java
public class OAuth2AuthenticationIT {
    
    private static Container container;
    private RestClient restClient;
    
    @BeforeAll
    static void setUp() {
        container = new OAuth2Container()
            .withUser("user", "password", "USER")
            .withUser("admin", "password", "ADMIN")
            .withOAuth2Client(
                // Confidential client for backend services
                new Client("backend-service", "backend-secret")
                    .withRedirectUris("http://localhost:8080/callback")
                    .withScopes("api", "user:read")
            )
            .withOAuth2Client(
                // Public client for frontend (PKCE required)
                new Client("frontend-app", "")
                    .withRedirectUris("http://localhost:5173", "http://localhost:8080")
                    .withScopes("openid", "profile", "email")
                    .withRequireProofKey(true)
            );
        container.start();
    }
    
    @BeforeEach
    void setup() {
        restClient = RestClient.create();
    }
    
    @AfterAll
    static void tearDown() {
        if (container != null) {
            container.stop();
        }
    }
    
    @Test
    void testDiscoveryEndpoint() throws Exception {
        var response = restClient.get()
            .uri(container.getAuthServerUrl() + "/.well-known/openid-configuration")
            .retrieve()
            .toEntity(String.class);
        
        assertEquals(200, response.getStatusCode().value());
        
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> config = mapper.readValue(response.getBody(), Map.class);
        
        assertTrue(config.containsKey("issuer"));
        assertTrue(config.containsKey("authorization_endpoint"));
        assertTrue(config.containsKey("token_endpoint"));
    }
    
    @Test
    void testAuthorizationEndpointRequiresAuth() throws Exception {
        WebClient webClient = new WebClient();
        String authUrl = container.getAuthServerUrl() + "/oauth2/authorize?" +
            "response_type=code&" +
            "client_id=frontend-app&" +
            "redirect_uri=http://localhost:5173&" +
            "scope=openid&" +
            "code_challenge=E9Mrozoa2owUednRPg8w_-dvznju3T92jVWswbCQQWE&" +
            "code_challenge_method=S256";
        
        var page = webClient.getPage(authUrl);
        
        assertTrue(page.getUrl().toString().contains("/login"));
    }

    @Test
    void testTokenExchangeWithClientSecret() throws Exception {
        // Confidential client can authenticate with secret
        var response = restClient.post()
            .uri(container.getAuthServerUrl() + "/oauth2/token")
            .header("Authorization", "Basic " + Base64.getEncoder()
                .encodeToString("backend-service:backend-secret".getBytes()))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue("grant_type=client_credentials&scope=api")
            .retrieve()
            .toEntity(String.class);
        
        assertEquals(200, response.getStatusCode().value());
    }
}
```

## Migration from Spring Boot 3.x

**Breaking Changes in Spring Boot 4.0.3 / Spring Security 7.0:**

1. **Public clients now require explicit PKCE configuration:**
   ```java
   // Old way (Spring Boot 3.x)
   new Client("frontend-client", null)  // Null secret

   // New way (Spring Boot 4.0.3)
   new Client("frontend-client", "")    // Empty string secret
       .withRequireProofKey(true)        // Explicitly enable PKCE
   ```

2. **Token endpoint auth methods are now explicit:**
   ```java
   // Default for confidential clients: "client_secret_basic"
   new Client("backend", "secret")

   // If you need POST method:
   new Client("backend", "secret")
       .withTokenEndpointAuthMethod("client_secret_post")

   // For public clients: auto-set to "none" when secret is empty
   new Client("frontend", "")  // Auth method automatically "none"
   ```

## Troubleshooting

### Container fails to start

Ensure Docker is running and accessible. TestContainers will automatically detect your Docker environment (Npipe on Windows, Unix socket on Linux/macOS).

Verify OAuth2 server image exists: `docker images | grep oauth2-server`

### Ports in use

TestContainers automatically selects available ports. If you get port errors, ensure you're not hardcoding ports in your tests - use `container.getAuthServerUrl()` instead.

### PKCE validation failures

If you see "code_challenge" parameter errors, ensure public clients have:
```java
.withRequireProofKey(true)  // Enable PKCE requirement
```

And test code includes PKCE parameters:
```java
"code_challenge=" + codeChallenge + "&code_challenge_method=S256"
```

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
    
    private static Container oauth2;
    
    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.authorizationserver.issuer", 
            oauth2::getIssuerUrl);
    }
    
    @BeforeAll
    static void setup() {
        oauth2 = new OAuth2Container()
            .withUser("testuser", "testpass", "USER");
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

## License

This library is part of the auth-server project.

# OAuth2 TestContainers Integration

OAuth2TestContainers is a reusable library that allows you to spin up an OAuth2 Authorization Server in Docker during testing, enabling integration tests for applications that depend on OAuth2 authentication.

## Overview

This library provides a TestContainers extension that:
- Manages the lifecycle of an OAuth2 Authorization Server Docker container
- Configures users and roles for authentication testing
- Registers OAuth2 clients programmatically
- Supports custom issuer URLs and context paths
- Loads configuration from YAML files
- Provides fluent builder API for easy setup

## Dependencies

Add to your `pom.xml`:

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

```java
container = new OAuth2Container()
    .withOAuth2Client(
        new OAuth2Client("client-id", "client-secret")
            .withRedirectUri("http://localhost:8080/callback")
            .withScopes("openid", "profile", "email")
    );
container.start();
```

### Loading Configuration from YAML Files

Create a YAML file at `src/test/resources/test-config.yaml`:

```yaml
app:
  security:
    users:
      - username: user1
        password: password1
        roles:
          - USER
      - username: admin
        password: password2
        roles:
          - ADMIN
          - USER
```

Then load it:

```java
container = new OAuth2Container()
    .withConfigFile("classpath:test-config.yaml");
container.start();
```

### Mixing File and Fluent Configuration

You can combine both approaches:

```java
container = new OAuth2Container()
    .withConfigFile("classpath:base-config.yaml")
    .withUser("extra-user", "extra-pass", "USER");  // Adds to file-based config
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

**Methods:**

- `withUser(String username, String password, String... roles)` - Add a user with roles
- `withOAuth2Client(OAuth2Client client)` - Register an OAuth2 client
- `withConfigFile(String filePath)` - Load configuration from YAML file
  - Supports `classpath:` prefix for resources on the classpath
  - Also supports filesystem paths
- `withIssuerUrl(String issuerUrl)` - Set custom issuer URL
- `withContextPath(String contextPath)` - Set custom context path
- `getAuthServerUrl()` - Get the server URL (http://localhost:mappedPort)
- `getIssuerUrl()` - Get the issuer URL (custom if set, otherwise auth server URL)
- `start()` - Start the container
- `stop()` - Stop the container
- `isRunning()` - Check if container is running

### OAuth2Client

Represents an OAuth2 client registration.

**Constructor:**
```java
new OAuth2Client(String clientId, String clientSecret)
```

**Methods:**

- `withRedirectUri(String uri)` - Add a redirect URI
- `withScopes(String... scopes)` - Add allowed scopes
- `withGrantTypes(String... grantTypes)` - Set allowed grant types

### OAuth2User

Represents a user (internal - created via `withUser()`).

Users include username, password, and a set of roles.

## Complete Example

```java
public class OAuth2AuthenticationIT {
    
    private static OAuth2Container container;
    private RestClient restClient;
    
    @BeforeAll
    static void setUp() {
        container = new OAuth2Container()
            .withUser("user", "password", "USER")
            .withUser("admin", "password", "ADMIN")
            .withOAuth2Client(
                new OAuth2Client("test-app", "test-secret")
                    .withRedirectUri("http://localhost:8080/callback")
                    .withScopes("openid", "profile", "email")
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
            "client_id=test-app&" +
            "redirect_uri=http://localhost:8080/callback&" +
            "scope=openid";
        
        var page = webClient.getPage(authUrl);
        
        assertTrue(page.getUrl().toString().contains("/login"));
    }
}
```

## Configuration File Format

YAML files should follow this structure:

```yaml
app:
  security:
    users:
      - username: user1
        password: password1
        roles:
          - USER
          - VIEWER
      - username: admin
        password: password2
        roles:
          - ADMIN
          - USER
```

## Troubleshooting

### Container fails to start

Ensure Docker is running and accessible. TestContainers will automatically detect your Docker environment (Npipe on Windows, Unix socket on Linux/macOS).

### Users not being loaded from file

- Verify the YAML file is on the classpath (in `src/test/resources`)
- Check that the YAML structure matches the expected format (see above)
- Ensure usernames and passwords don't contain special characters that need escaping

### Ports in use

TestContainers automatically selects available ports. If you get port errors, ensure you're not hardcoding ports in your tests - use `container.getAuthServerUrl()` instead.

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

## License

This library is part of the auth-server project.

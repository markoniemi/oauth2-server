# OAuth2 Authorization Server

A Spring Boot 3.5.6 OAuth2 Authorization Server with integrated TestContainers support for testing downstream applications.

## Quick Start

### Run the Server

```bash
mvn -pl auth-server spring-boot:run
```

Server runs on `http://localhost:9000`

No users are configured by default. Add them in `config/application.yaml` (or any Spring property source):

```yaml
app:
  security:
    users:
      - username: admin
        password: admin              # plain text, or encoded e.g. "{bcrypt}$2a$10$..."
        roles: [USER, ADMIN]
```

A public `frontend-client` (PKCE, redirect URIs `http://localhost:8080` and `http://localhost:5173`) is registered by default; more clients go under `spring.security.oauth2.authorizationserver.client.*`.

### Use in Tests

```java
@BeforeAll
static void setUp() {
    container = new OAuth2Container()
        .withUser("testuser", "testpass", "USER")
        .withOAuth2Client(
            new Client("client-id", "client-secret")
                .withRedirectUris("http://localhost:8080/callback")
                .withScopes("openid", "profile")
        );
    container.start();
}

String authServerUrl = container.getAuthServerUrl();
```

## Documentation

- **[docs/OAuth2TestContainersUsage.md](docs/OAuth2TestContainersUsage.md)** — Complete guide to using OAuth2TestContainers library for testing
- **[docs/TechSpec.md](docs/TechSpec.md)** — Architecture and configuration details of the auth server
- **[.claude/CLAUDE.md](.claude/CLAUDE.md)** — Development guidelines and coding standards

## Features

### Authorization Server
- OAuth2 Authorization Code flow with PKCE support
- OpenID Connect discovery endpoint
- JWT token generation
- User authentication with form login
- Custom user and role management

### TestContainers Integration
- Spin up auth server in Docker during tests
- Fluent API for configuration
- User and OAuth2 client registration
- Custom issuer URL and context path support

## Architecture

Two Maven modules under `auth-server-parent`:

```
auth-server/                         # Spring Boot app, Docker image (jib)
├── src/main/java/com/example/auth/
│   ├── AuthServerApplication.java
│   └── config/
│       ├── CorsProperties.java
│       ├── SecurityConfig.java
│       └── SecurityProperties.java
├── src/main/resources/application.yaml
└── src/test/java/com/example/auth/
    ├── AuthServerIT.java
    └── config/SecurityConfigTest.java
auth-server-testcontainers/          # Testcontainers library (no server code)
├── src/main/java/com/example/auth/testcontainers/
│   ├── OAuth2Container.java
│   ├── Client.java
│   └── User.java
├── src/test/java/com/example/auth/testcontainers/   # *IT run against the image built by auth-server
└── src/test/resources/test-config.yaml
```

`auth-server` is listed first in the reactor: it builds the image (`jib:dockerBuild`, pre-integration-test) that the library's integration tests start.

## Testing

Run all tests (builds the local image first):
```bash
mvn verify
```

Run one module or test class:
```bash
mvn -pl auth-server-testcontainers verify -Dit.test=ContainerIT
```

### Test Coverage

- `auth-server`: 5 unit tests (users, password encoding) + 5 integration tests (auth code + PKCE, logout)
- `auth-server-testcontainers`: 25 unit tests (generated config, `Client`, `User`) + 21 integration tests against the container

## Requirements

- Java 21+
- Maven 3.8+
- Docker (for TestContainers integration tests)

## Technologies

- Spring Boot 3.5.6
- Spring Security OAuth2 Authorization Server
- Spring Web
- Jackson (with YAML support)
- TestContainers
- JUnit 5
- Lombok

## Key Endpoints

- **POST** `/oauth2/token` — Token endpoint
- **GET** `/.well-known/openid-configuration` — OpenID Connect discovery
- **GET** `/oauth2/authorize` — Authorization endpoint
- **POST** `/login` — Login form submission
- **GET** `/login` — Login page

## Development Guidelines

See [.github/copilot-instructions.md](.github/copilot-instructions.md) for:
- Coding conventions and patterns
- Error handling standards (RFC 7807)
- Validation framework usage
- Git commit message format

## Project Structure

This is a monolithic Spring Boot application containing:
- **Backend**: Java/Spring Boot OAuth2 Authorization Server
- **Database**: PostgreSQL (prod) / H2 (test)
- **Authentication**: OAuth2 with Spring Security

## Git Workflow

Commit messages use a **one-line format with semicolons**:
```
Brief action; additional change; optional note
```

Example:
```
Add OAuth2TestContainers library; support fluent builder and YAML config
```

## License

Part of the oauth2-server project.

## More Information

- **Development guidelines**: [.claude/CLAUDE.md](.claude/CLAUDE.md)
- **Tech specification**: [docs/TechSpec.md](docs/TechSpec.md)

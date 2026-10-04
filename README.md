# OAuth2 Authorization Server

A Spring Boot 4.0.3 OAuth2 Authorization Server with integrated TestContainers support for testing downstream applications.

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
        .withConfigFile("oauth2-server.yaml");  // users and clients, overrides the bundled defaults
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
- Bundled defaults (public `frontend-client`), overridden by a Spring Boot config file
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
│   └── OAuth2Container.java
├── src/test/java/com/example/auth/testcontainers/   # *IT run against the image built by auth-server
└── src/test/resources/                              # config files mounted by the ITs
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
- `auth-server-testcontainers`: 20 integration tests against the container (bundled defaults, config files, issuer and context path)

## Requirements

- Java 21+
- Maven 3.8+
- Docker (for TestContainers integration tests)

## Technologies

- Spring Boot 4.0.3
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

## Project Structure

A multi-module Maven project:
- **auth-server**: Spring Boot OAuth2 Authorization Server, packaged as a Docker image
- **auth-server-testcontainers**: Testcontainers library for running the server in downstream tests

## More Information

- **Development guidelines**: [.claude/CLAUDE.md](.claude/CLAUDE.md)
- **Tech specification**: [docs/TechSpec.md](docs/TechSpec.md)

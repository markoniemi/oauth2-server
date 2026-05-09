# OAuth2 Authorization Server

A Spring Boot 3.5.6 OAuth2 Authorization Server with integrated TestContainers support for testing downstream applications.

## Quick Start

### Run the Server

```bash
mvn spring-boot:run
```

Server runs on `http://localhost:9000`

**Default credentials:**
- Username: `user`
- Password: `user`

### Use in Tests

```java
@BeforeAll
static void setUp() {
    container = new OAuth2Container()
        .withUser("testuser", "testpass", "USER")
        .withOAuth2Client(
            new OAuth2Client("client-id", "client-secret")
                .withRedirectUri("http://localhost:8080/callback")
                .withScopes("openid", "profile")
        );
    container.start();
}

String authServerUrl = container.getAuthServerUrl();
```

## Documentation

- **[docs/OAuth2TestContainersUsage.md](docs/OAuth2TestContainersUsage.md)** — Complete guide to using OAuth2TestContainers library for testing
- **[docs/TechSpec.md](docs/TechSpec.md)** — Architecture and configuration details of the auth server
- **[.github/copilot-instructions.md](.github/copilot-instructions.md)** — Development guidelines and coding standards

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
- YAML-based configuration support
- Custom issuer URL and context path support

## Architecture

```
src/
├── main/
│   ├── java/com/example/auth/
│   │   ├── config/
│   │   │   ├── AuthorizationServerConfig.java
│   │   │   ├── SecurityConfig.java
│   │   │   └── SecurityProperties.java
│   │   ├── testcontainers/
│   │   │   ├── OAuth2Container.java
│   │   │   ├── OAuth2Client.java
│   │   │   ├── OAuth2User.java
│   │   │   ├── OAuth2ServerConfig.java
│   │   │   └── OAuth2ContainerRegisteredClientConfig.java
│   │   └── controller/
│   └── resources/
│       └── application.yaml
└── test/
    ├── java/com/example/auth/
    │   ├── testcontainers/
    │   │   ├── OAuth2ContainerIT.java
    │   │   ├── OAuth2ContainerClientsIT.java
    │   │   ├── OAuth2ContainerConfigFileIT.java
    │   │   └── OAuth2ContainerAuthFlowIT.java
    │   └── AuthServerIT.java
    └── resources/
        └── test-config.yaml
```

## Testing

Run all tests:
```bash
mvn verify
```

Run specific test class:
```bash
mvn test -Dtest=OAuth2ContainerIT
```

### Test Coverage

- **58 tests** — All passing
  - 21 unit tests for data classes and builders
  - 21 integration tests for complete auth server feature
  - 16 container-specific integration tests

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

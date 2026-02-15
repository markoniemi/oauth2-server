# Auth Server Module Context

## Overview
This module is the OAuth2 Authorization Server. It handles user authentication and issues JWT tokens.

## Key Technologies
- Spring Boot 3.5.6
- Spring Security OAuth2 Authorization Server
- Java 21

## Configuration
- **Port**: 9000
- **Context Path**: `/`
- **Issuer URI**: `http://localhost:9000`

## Key Files
- `src/main/java/com/example/auth/config/AuthorizationServerConfig.java`: Configures the authorization server, registered clients, and token settings.
- `src/main/java/com/example/auth/config/SecurityConfig.java`: Configures standard Spring Security settings (e.g., form login).
- `src/main/resources/application.yaml`: Application properties.

## Development
- Run with `mvn spring-boot:run`.
- Default credentials: `user` / `password`.

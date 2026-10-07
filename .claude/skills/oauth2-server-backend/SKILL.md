---
name: oauth2-server-backend
description: Use when working on the auth-server module of oauth2-server — Spring Authorization Server configuration, security filter chains, clients, users, CORS, issuer, JWT/OIDC endpoints, application.yaml, the Docker image, or fixing bugs and adding features in the server.
---

# OAuth2 Server — auth-server module

## Overview

`auth-server` is a Spring Boot OAuth2 Authorization Server (OIDC, JWT, form login) shipped as a Docker image `ghcr.io/markoniemi/oauth2-server`. Downstream apps (dynamic-form) run it in docker-compose and in tests via `auth-server-testcontainers` — see the `oauth2-server-testcontainers` skill.

**Core principle:** let Spring Boot auto-configuration do the work. Clients, signing keys (`JWKSource`) and `AuthorizationServerSettings` come from properties; only add beans for what auto-configuration cannot express.

## Technology Stack

- Spring Boot 4.0.3, Spring Security 7 (authorization server built in), Java 21
- `spring-boot-starter-security-oauth2-authorization-server`, Actuator
- Lombok, Apache Commons Collections 4
- Tests: JUnit 5, Spring Boot Test, HtmlUnit
- Image: Jib (`gcr.io/distroless/java21-debian12` base)

## Project Structure

```
auth-server/src/main/java/com/example/auth/
  AuthServerApplication.java
  config/
    SecurityConfig.java       # two filter chains, users, password encoder, CORS
    SecurityProperties.java   # app.security.users (record)
    CorsProperties.java       # app.cors.allowed-origins (record)
auth-server/src/main/resources/application.yaml   # bundled defaults (frontend-client, CORS, port 9000)
auth-server/src/test/java/com/example/auth/
  AuthServerIT.java           # full browser flows with HtmlUnit
  PkceUtil.java
  config/SecurityConfigTest.java
```

## Configuration Model

| What | Where | Notes |
|---|---|---|
| Clients | `spring.security.oauth2.authorizationserver.client.<id>.*` | Registered by Spring Boot. Default `frontend-client`: public, PKCE required, redirect `localhost:8080` / `5173` |
| Users | `app.security.users` | `SecurityProperties` → `InMemoryUserDetailsManager`; bundled list is empty |
| Passwords / secrets | user `password`, client `client-secret` | `{bcrypt}...` style prefix, or plain text (NoOp fallback in `passwordEncoder()`) |
| CORS | `app.cors.allowed-origins` | Applied to both filter chains |
| Issuer | `spring.security.oauth2.authorizationserver.issuer` | Unset = derived from request `Host`. Set `SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER` when clients reach the server under different hostnames (browser `localhost:9000` vs backend `auth:9000`) |
| Context path | `server.servlet.context-path` | Default `/` |

Signing keys are generated at startup: tokens do not survive a restart.

## Filter Chains (`SecurityConfig`)

1. `@Order(1)` authorization server endpoints (`/oauth2/**`, `/.well-known/**`, `/userinfo`, `/connect/logout`), OIDC enabled, HTML requests redirect to `/login`, JWT resource server for userinfo.
2. `@Order(2)` everything else: `/actuator/**` public, rest authenticated, form login.

## Development Workflow

Run from the repository root.

```bash
mvn -pl auth-server spring-boot:run      # server on http://localhost:9000
mvn -pl auth-server test                 # unit tests
mvn -pl auth-server verify               # + AuthServerIT, builds Docker image (Docker required)
```

`verify` runs Jib `dockerBuild` in `pre-integration-test`, so the local `:latest` image is the code under test for the testcontainers module.

## Coding Conventions

- Google Java Style, two-space indentation, static imports for helpers (`withDefaults`, `emptyIfNull`)
- Configuration properties are **records** with `@ConfigurationProperties`; normalize nulls in the compact constructor with `emptyIfNull`
- Never log secrets: override `toString()` on records holding passwords
- Comments explain *why* (security behaviour, ordering), not what
- Constructor or `@Bean` method parameter injection; no field injection
- Prefer properties over code for clients and server settings; changing a property name breaks downstream config files

## Clean Code and Effective Java

- One reason to change per class/method; methods ≈20 lines max; no duplication
- Intention-revealing names; comments last resort
- Prefer immutability (records), composition over inheritance, interfaces over abstract classes
- Static factories / builders for many parameters; never `new` a dependency
- Validate parameters early; return empty collections or `Optional`, never `null`
- Favor standard exceptions (`IllegalArgumentException`, `IllegalStateException`); never swallow exceptions
- Use the standard library and Commons instead of hand-rolled helpers

## Testing

- Unit tests for beans with logic (`SecurityConfigTest`: password matching, roles, `toString`)
- `AuthServerIT`: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + HtmlUnit `WebClient` driving authorize → login → token, logout, PKCE rejection
- Test names describe behaviour: `performAuthorizationRequestWithoutCodeChallengeIsRejected`, `plainValuesMatchAsPlainText`

## Common Mistakes

- Defining a `RegisteredClientRepository` or `JWKSource` bean — overrides auto-configuration and the property-based client merge
- Changing claims, scopes, endpoints or default client without checking dynamic-form (see CLAUDE.md)
- Running `mvn verify` without Docker — Jib `dockerBuild` fails
- Expecting a fixed `iss` claim while the issuer is unset

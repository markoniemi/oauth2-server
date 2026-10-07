---
name: oauth2-server-testcontainers
description: Use when working on the auth-server-testcontainers library (OAuth2Container), its config-file merging, its integration tests, or when a downstream project's tests that start the auth server in Docker fail.
---

# OAuth2 Server — auth-server-testcontainers library

## Overview

Library artifact `com.example:auth-server-testcontainers`. `OAuth2Container` extends `GenericContainer<OAuth2Container>` and runs the `ghcr.io/markoniemi/oauth2-server:latest` image so downstream projects (dynamic-form `TestcontainersConfig`) can test against a real authorization server.

**Core principle:** the library never depends on server code. The only contract with `auth-server` is Spring property names and the image. It is a public API: keep it backward compatible.

Full user guide: `docs/OAuth2TestContainersUsage.md`. Config file details: `docs/ConfigFileSupport.md`.

## API

| Method | Effect |
|---|---|
| `new OAuth2Container()` / `new OAuth2Container(DockerImageName)` | Default image or pinned tag; exposes 9000, waits for `/actuator/health` (2 min) |
| `withConfigFile(classpathResource)` | Mounts YAML as `/config/application.yaml` |
| `withIssuerUrl(url)` | Sets `SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER` (fixed `iss`) |
| `withContextPath(path)` | Sets `SERVER_SERVLET_CONTEXT_PATH`, moves health check |
| `getAuthServerUrl()` | `http://localhost:<mapped port><context path>` |
| `getIssuerUrl()` | Configured issuer, else `getAuthServerUrl()` |

## Config File Merge Rules

The mounted file overrides the server's bundled `application.yaml`:
- **Maps merge by key** — clients under `spring.security.oauth2.authorizationserver.client.*` add to the bundled `frontend-client`
- **Single values and lists are replaced** — `app.security.users`, `app.cors.allowed-origins`, a client's `redirect-uris`

Users and clients are configured only through the config file (no fluent API).

## Development Workflow

Run from the repository root. The ITs use the local `:latest` image, built by `auth-server` during the same reactor run:

```bash
mvn verify        # builds auth-server image (Jib dockerBuild), then runs library ITs; Docker required
```

Running only `-pl auth-server-testcontainers verify` tests against whatever `:latest` image is already in the local Docker daemon — possibly stale.

## Tests

`src/test/java/com/example/auth/testcontainers/`:
- `ContainerIT` — bundled defaults only
- `ContainerClientsIT`, `ConfigFileIT` — config file merging (`test-config.yaml`, `clients-config.yaml`)
- `ContainerAuthFlowIT`, `ConfigFileAuthFlowIT` — full login/token flow with HtmlUnit

Start the container in `@BeforeAll`, stop it in `@AfterAll`.

## Coding Conventions

Same as the `oauth2-server-backend` skill. Additionally:
- Fluent `with*` methods return `this` (via `withEnv` / `withCopyFileToContainer`)
- Javadoc on every public method — it is the library's API surface
- No dependency on `auth-server` classes

## Common Mistakes

- Renaming or removing a public method — breaks dynamic-form tests; deprecate instead
- Assuming a list in the config file appends to the bundled list — it replaces it
- Trusting IT results after running only this module — rebuild the image first
- Hardcoding the mapped port — always use `getAuthServerUrl()`

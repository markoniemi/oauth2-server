# OAuth2 Authorization Server Technical Specification

## Overview

OAuth2 Authorization Server providing OpenID Connect (OIDC) discovery, JWT token generation, and user authentication for downstream applications. Supports both confidential and public clients with PKCE (Proof Key for Code Exchange).

## Key Technologies

- **Spring Boot**: 4.0.3
- **Spring Security**: 7.0
- **Spring Authorization Server**: Integrated with Spring Security 7.0
- **Java**: 21
- **JWT**: RS256 (RSA key pair auto-generated at startup)

## Configuration

- **Port**: 9000
- **Context Path**: `/`
- **Issuer URI**: `http://localhost:9000`
- **Key Generation**: RSA 2048-bit keys auto-generated and stored in memory

## OAuth2 Endpoints

- **Authorization**: `GET /oauth2/authorize`
- **Token**: `POST /oauth2/token`
- **Token Introspection**: `POST /oauth2/introspect`
- **Token Revocation**: `POST /oauth2/revoke`
- **JWKS**: `GET /oauth2/jwks`
- **User Info**: `GET /userinfo`
- **Logout**: `POST /connect/logout`

## OIDC Discovery

- **Metadata Endpoint**: `GET /.well-known/openid-configuration`
- **Supported Scopes**: `openid`, `profile`, `email`
- **Grant Types**: `authorization_code`, `refresh_token`, `client_credentials`
- **Client Auth Methods**: `client_secret_basic`, `client_secret_post`, `none` (for PKCE)

## Configuration Classes

- `SecurityConfig.java`: Dual filter chains
  - Chain 1: OAuth2 authorization server + OIDC (matches `/oauth2/**`, `/.well-known/**`)
  - Chain 2: API security with form login (catches all other requests)
- Signing keys (`JWKSource`) and `AuthorizationServerSettings`: provided by Spring Boot auto-configuration; issuer from `spring.security.oauth2.authorizationserver.issuer` (unset = derived from request)
- Clients: registered by Spring Boot from `spring.security.oauth2.authorizationserver.client.*` properties (default `frontend-client` in `application.yaml`)
- `CorsProperties.java`: allowed CORS origins (`app.cors.allowed-origins`)
- `SecurityProperties.java`: User configuration from `app.security.users` YAML

## Client Support

**Confidential Clients:**
- Client secret required
- Auth methods: `client_secret_basic` (default), `client_secret_post`
- Grant types: authorization_code, refresh_token, client_credentials

**Public Clients (SPA/Native Apps):**
- No client secret (empty string in Client constructor)
- Auth method: `none` (auto-set for empty secrets)
- PKCE required (set via `withRequireProofKey(true)`)
- Grant types: authorization_code with PKCE, refresh_token

## Token Configuration

- **Access Token TTL**: 1 hour
- **Refresh Token TTL**: 7 days
- **Token Signing**: RS256 with auto-generated RSA key pair
- **Token Issuer**: Configured via issuer-uri property

## User Authentication

- Form-based login at `/login`
- Users configured via `app.security.users` YAML property
- Roles: User-defined (e.g., `USER`, `ADMIN`)
- Password encoding: NoOp (dev/test only, **DO NOT USE IN PRODUCTION**)

## Docker Support

- JIB plugin builds multi-architecture images
- Container publishes on port 9000
- Health check: `GET /actuator/health`
- Startup time: ~5-7 seconds
- TestContainers integration via `OAuth2Container` class

## Development

```bash
# Run locally
mvn spring-boot:run

# Build Docker image
mvn package jib:dockerBuild

# Test with Docker
docker run -d -p 9000:9000 ghcr.io/markoniemi/oauth2-server:latest
curl http://localhost:9000/.well-known/openid-configuration
```

## Spring Boot 4.0.3 → Spring Security 7.0 Migration Notes

- **API Change**: `OAuth2AuthorizationServerConfiguration.applyDefaultSecurity()` → `http.oauth2AuthorizationServer()` configurer
- **Filter Chains**: Must use `securityMatcher()` to avoid UnreachableFilterChainException with multiple chains
- **Public Clients**: Now require explicit `ClientAuthenticationMethod.NONE` for PKCE flows
- **Properties Migrator**: Use spring-boot-properties-migrator during upgrade to identify deprecated properties

## Default Credentials (Development Only)

- **Username**: `admin`
- **Password**: `admin`
- **Roles**: `ADMIN`, `USER`

## Security Notes

- **Private Key Storage**: In-memory only (regenerated on each startup). For production, use KeyStore.
- **CORS**: Configured to allow redirect URIs from registered clients
- **CSRF**: Enabled except for token endpoints
- **PKCE**: Enforced for public clients (authorization_code grant)

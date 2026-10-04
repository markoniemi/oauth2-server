# Config File Support: Replace the Fluent API with `withConfigFile`

Status: **implemented** on branch `config-file-support` (2026-10-04).

## Goal

Make the server's own Spring configuration file the only way to configure users and clients in
`auth-server-testcontainers`. Drop the fluent `withUser` / `withOAuth2Client` API together with the
`Client` and `User` classes and the YAML generation behind them.

## Changes

### Removed

- `Client.java` and `User.java` entirely.
- In `OAuth2Container` (about 140 of 229 lines):
  - `withUser`, `withOAuth2Client`
  - `getUsers`, `getClients`
  - `configure()`, `generateConfigYaml()`, `corsOrigins()`, `toClientProperties()`
- Dependencies of `auth-server-testcontainers`: `jackson-dataformat-yaml`, `commons-lang3` and
  possibly Lombok. Only `testcontainers` remains.
- Tests: `ClientTest`, `UserTest`, `OAuth2ContainerConfigTest`.

### Kept

These are set through environment variables, not the YAML, so they still work alongside a config
file:

- Both constructors and `waitForHealth`
- `withConfigFile`
- `withIssuerUrl`, `withContextPath`
- `getAuthServerUrl`, `getIssuerUrl`

The result is roughly 50 lines.

### Integration tests to rewrite

Each needs a YAML resource in place of the fluent setup:

| Test | Uses | Action |
|------|------|--------|
| `ContainerIT` | `withUser`; `yamlGenerationIncludesAllUsers` | Use a config file; drop the YAML-generation test |
| `ContainerClientsIT` | `withUser`, `withOAuth2Client` (client credentials) | New resource with a confidential client |
| `ContainerAuthFlowIT` | `withOAuth2Client` | New resource, or merge into `ConfigFileAuthFlowIT` |
| `ConfigFileIT` | `fluentApiAndConfigFileMixed` | Delete that test; keep the rest |
| `ConfigFileAuthFlowIT` | `withConfigFile` only | No change |

## Defaults and Overrides

The server's bundled `auth-server/src/main/resources/application.yaml` acts as the defaults file.
The file mounted by `withConfigFile` lands at `/config/application.yaml`, which Spring Boot reads
after the bundled one, so its values win. No new defaults file and no changes to the server are
needed.

The bundled defaults already include:

- A public `frontend-client`: auth method `none`, `authorization_code` only, `require-proof-key:
  true`, scopes `openid profile email`, a 1h access token, and redirect and post-logout URIs for
  `http://localhost:8080` and `http://localhost:5173`.
- `app.cors.allowed-origins` for the same two origins.
- No users (`app.security.users: []`), so local and docker-compose runs never ship default
  credentials.

How the mounted file is merged with the defaults:

| Kind of value | Behavior | Example |
|---------------|----------|---------|
| Maps | Merged by key | A client under `client.<id>` is added, or overrides single fields of an existing client with the same id |
| Single values | Replaced | `require-proof-key`, `access-token-time-to-live` |
| Lists | Replaced whole, never merged | `app.security.users`, `app.cors.allowed-origins`, `redirect-uris` |

Consequences:

- To adjust `frontend-client`, use the same key (`client.frontend-client`). The fields given are
  overridden and the rest comes from the defaults.
- A client under a new key gets no defaults: it needs a full registration (auth methods, grant
  types, scopes).
- A list in the mounted file must hold every entry, not only the additions.
- A default cannot be removed: `frontend-client` is always registered. This is harmless for tests.

Test-only defaults (such as default users) could later go into a server profile,
`application-testcontainers.yaml`, enabled by `OAuth2Container` through `SPRING_PROFILES_ACTIVE`.
The mounted file would still override it. This isn't needed while consumers supply their own users.

## Trade-offs

The library currently encodes knowledge about the server that config files would then have to
repeat by hand. The bundled defaults cover the common public-client setup, so this mainly affects
clients that differ from `frontend-client`:

- **CORS:** `app.cors.allowed-origins` is derived from the redirect URIs today. Origins that differ
  from the defaults must be listed explicitly.
- **Public clients:** an empty secret currently sets the auth method to `none`, removes
  `refresh_token` (the server never issues it to public clients) and forces
  `require-proof-key: true`. `frontend-client` gets this from the defaults; other public clients
  must spell it out.
- **Fail-fast checks:** "no usable grant types" and "authorization_code without redirect URIs" fail
  immediately today. A broken YAML file instead shows up as the server refusing to start: a 2-minute
  startup timeout followed by digging through container logs.
- **Validation:** blank usernames, passwords and client IDs are no longer caught before the
  container starts.
- **Inline setup:** every distinct test setup needs its own YAML resource.

**Recommendation:** go ahead if the server's config format is the API to support. There is one
consumer (dynamic-form), its setup is static, and the YAML is plain Spring Boot configuration.
Keep the fluent API instead if more consumers or setups that vary per test are expected, because
that is where the public-client handling and fail-fast checks pay off.

## dynamic-form Migration (breaking change)

`backend/src/test/java/com/example/backend/config/TestcontainersConfig.java` uses `withUser` and
`withOAuth2Client`. Replace that setup with a test resource `oauth2-server.yaml`. Users and CORS
origins replace the defaults. The client is spelled out in full rather than relying on the default
`frontend-client`, so renaming it later doesn't silently lose its auth method, grant types, scopes
or PKCE:

```yaml
app:
  security:
    users:
      - { username: admin, password: admin, roles: [USER, ADMIN] }
      - { username: user, password: user, roles: [USER] }
  cors:
    allowed-origins: [http://localhost:8080, http://localhost:5173]
spring:
  security:
    oauth2:
      authorizationserver:
        client:
          frontend-client:
            registration:
              client-id: frontend-client
              client-authentication-methods: [none]
              authorization-grant-types: [authorization_code]
              redirect-uris: [http://localhost:8080, http://localhost:5173]
              post-logout-redirect-uris: [http://localhost:8080, http://localhost:5173]
              scopes: [openid, profile, email]
            require-proof-key: true
            token:
              access-token-time-to-live: 1h
```

While the key is `frontend-client`, these values override the default client with the same values.
If dynamic-form renames the client, the block still works as a complete registration, and the
default `frontend-client` simply stays registered alongside it.

and the container setup becomes:

```java
new OAuth2Container()
    .withConfigFile("oauth2-server.yaml")
    .withLogConsumer(new Slf4jLogConsumer(log))
    .withCreateContainerCmdModifier(getPortConfig());
```

Run the dynamic-form backend test suite against a locally built image before publishing.

## Documentation to update

- `README.md`: the "Use in Tests" example and the TestContainers feature list.
- `docs/OAuth2TestContainersUsage.md`: replace the fluent API reference with config file examples
  (public client, confidential client, users), list the bundled defaults and explain the merge
  rules from "Defaults and Overrides".
- `.claude/CLAUDE.md`: the "Client & User" key component entry.
- `RELEASE_NOTES.md`: note the breaking change.

## Steps

1. Add YAML resources for the rewritten integration tests.
2. Rewrite `ContainerIT`, `ContainerClientsIT` and `ContainerAuthFlowIT`, and trim `ConfigFileIT`.
3. Remove `Client`, `User`, their unit tests and `OAuth2ContainerConfigTest`.
4. Strip `OAuth2Container` down to the kept methods.
5. Remove the unused dependencies from `auth-server-testcontainers/pom.xml`.
6. Run `mvn verify` (needs Docker).
7. Update the docs listed above.
8. Migrate dynamic-form and run its backend test suite.

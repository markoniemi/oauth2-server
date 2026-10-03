# Simplification & Configurability Plan

**Date:** 2026-10-03
**Baseline:** Spring Boot 4.0.3, Spring Security 7.0, commit `ac2c60d`
**Status:** In progress

## Goal

Lean on Spring Boot 4's authorization server auto-configuration instead of hand-written beans, fix the bugs this hides, and make hard-coded values configurable. Each phase is independently shippable and must be validated against dynamic-form before the next one starts.

## Key findings driving this plan

| # | Finding | Location |
|---|---------|----------|
| F1 | Custom `RegisteredClientRepository` bean makes Boot ignore `spring.security.oauth2.authorizationserver.client.*`. Clients from `withOAuth2Client(...)` and YAML config files are never registered; the hard-coded `frontend-client` always is. dynamic-form only works because its client matches the hard-coded one. | `ClientConfig.java:37` |
| F2 | Custom `AuthorizationServerSettings` bean hard-codes issuer `http://localhost:9000`, overriding the `issuer` property. `withIssuerUrl()` is never sent to the container; `contextPath` is unused. | `OAuth2AuthServerConfig.java:27`, `OAuth2Container.java:48`, `ServerConfig.java:14` |
| F3 | Custom `JWKSource` duplicates `OAuth2AuthorizationServerJwtAutoConfiguration`. | `OAuth2AuthServerConfig.java:31-56` |
| F4 | Container writes `registration.client-settings.require-proof-key` (wrong path; Boot expects `client.<id>.require-proof-key`). | `OAuth2Container.java:124` |
| F5 | `Client` has no post-logout redirect URIs; dynamic-form sends `post_logout_redirect_uri=window.location.origin`. | `Client.java` |
| F6 | Public clients (empty secret) are written with `client_secret_basic`. Only `ClientConfig` rewrites this to `none`. | `OAuth2Container.java:119` |
| F7 | CORS origins come from static `ClientConfig` state, which is always empty inside the container, so the hard-coded fallback is always used. | `SecurityConfig.java:113-145` |
| F8 | Local `mvn verify` runs `jib:build`, which **pushes** to ghcr.io instead of building a local image. | `pom.xml` jib execution |
| F9 | ITs accept 4xx responses, so F1 is not detected. | `ContainerClientsIT.java:55`, `ConfigFileAuthFlowIT.java:106` |

## How dynamic-form consumes this project

Validation steps below depend on these touch points:

- **Library:** `com.example:auth-server:0.1-SNAPSHOT` (test scope) → `OAuth2Container`, `Client` in `backend/src/test/java/com/example/backend/config/TestcontainersConfig.java`.
  - Registers `frontend-client` with empty secret, redirect URIs `http://localhost:8080` + `http://localhost:5173`, scopes `openid profile email`, PKCE.
  - Binds host port 9000 → issuer seen by tests is `http://localhost:9000`.
- **Image:** `ghcr.io/markoniemi/oauth2-server:latest` (Testcontainers and `docker-compose.yaml`).
- **Backend:** `issuer-uri: http://localhost:9000` (dev/test), `${OAUTH2_ISSUER_URI}` = `http://auth:9000` (compose/prod).
- **Frontend:** OIDC authority from `/api/config/oauth2-issuer-uri`; logout uses `post_logout_redirect_uri: window.location.origin`.
- **Stale:** `backend/src/test/resources/oauth2-config.yaml` uses a `users:`/`clients:` format nothing reads — candidate for deletion in dynamic-form.

## Standard validation procedure (run after every phase)

1. In oauth2-server:
   ```bash
   mvn clean install -Djib.skip          # unit + ITs, installs library jar to ~/.m2
   mvn jib:dockerBuild                   # local image ghcr.io/markoniemi/oauth2-server:latest
   ```
   (After Phase 0, plain `mvn clean install` does both.)
2. In dynamic-form:
   ```bash
   cd backend && mvn clean verify        # unit + ITs incl. e2e/FrontendIT against the container
   cd ../frontend && npm test
   ```
3. Manual smoke test (`docker compose up` or dev profile):
   - Login as `admin/admin` from `http://localhost:5173` and `http://localhost:8080`.
   - Call a protected API (token accepted by backend → issuer matches).
   - Logout → lands back on the app origin (not an error page).
   - Wait past access token lifetime → session behaves as before (silent renew / re-login).
4. Record result in the phase checklist below. Do not start the next phase on red.

## Per-phase workflow: commit, push, validate

Every phase ends with a commit and push, gated by dynamic-form validation:

1. **Implement** the phase tasks.
2. **Validate locally** with the standard procedure above (oauth2-server build + local image → dynamic-form tests + smoke test). Do not commit on red.
3. **Commit oauth2-server** to `master`, one-line message per `.claude/CLAUDE.md` (imperative, semicolon-separated, no period), e.g.
   `Fix OAuth2Container client properties; add post-logout redirect URIs and token TTLs`
4. **Push oauth2-server.** CI (`maven-build.yaml`) then:
   - runs all tests,
   - deploys `auth-server:0.1-SNAPSHOT` to repsy,
   - pushes `ghcr.io/markoniemi/oauth2-server:latest`,
   - dispatches `oauth2-server-built` to dynamic-form.
   Pushing publishes the image that `docker-compose.yaml` and other consumers pull — this is why local validation (step 2) must pass first.
5. **Commit + push dynamic-form** changes, if the phase has any (Phases 1 and 6). Push only after step 4's CI has deployed the new library snapshot, because dynamic-form CI resolves `auth-server` from repsy.
6. **Validate in CI**: oauth2-server workflow green, and the dispatched dynamic-form build green against the published image/snapshot.
7. **Log** commit hashes and results in the progress log. Only then start the next phase.

If step 6 fails: fix forward with a new commit, or `git revert` the phase commit and push (republishes previous behavior as `latest`).

---

## Pre-Phase — Claude Code environment setup

**Goal:** Make the Claude Code tooling for this repo correct before implementation starts. No code or behavior change.

### Tooling status (checked 2026-10-03)

| Tool | Status | Role in this plan |
|------|--------|-------------------|
| IntelliJ MCP (`mcp__idea__*`) | Connected; serves both oauth2-server and dynamic-form | Primary tool for build, diagnostics, run configurations, renames (per `.claude/CLAUDE.md`) |
| Context7 MCP | Available, allowed | Verify Boot 4 property names (Phases 2–3) |
| Playwright MCP (plugin) | Available | Optional: manual login/logout/issuer smoke test (Phases 2–3) |
| Caveman plugin | Active; `caveman-shrink` MCP fails to connect | Not required; fix or disable `caveman-shrink` |
| Superpowers plugin | Enabled | Phase-by-phase execution |
| Docker / Maven / Java / Node | 29.2 / 3.9.14 / 21.0.1 / 24.13 | Docker must be running for ITs and `jib:dockerBuild` |

### Tasks
- [ ] **Stop hook** in `.claude/settings.json` is copied from dynamic-form (`cd frontend && npm run lint:fix ... cd ../backend && mvn compile`); no `frontend/` or `backend/` here. Replace with `mvn -q compile`.
- [~] **`.env` read block** *(claude.json deleted; `deny` rule pending — user)*: `.claude/claude.json` is not a Claude Code config file, so its block never applies. Delete it and add to `settings.json`:
  ```json
  "deny": ["Read(./.env)", "Read(./.env.*)"]
  ```
- [ ] **Playwright permissions**: allowlist uses `mcp__playwright__*`, but plugin tools are `mcp__plugin_playwright_playwright__*`. Fix the names. Remove `enabledMcpjsonServers: ["playwright"]` (no `.mcp.json` in this repo).
- [ ] **IntelliJ MCP permissions**: add `mcp__idea__*` to the allowlist to avoid a prompt per call.
- [~] **Remove unrelated leftovers**: `aws` / `terraform` permissions *(pending — user)*, `.claude/skills/terraform.yaml`, and the dynamic-form skills duplicated in `.claude/skills/` plus `.claude/commands/test.md` (keep them in dynamic-form only) *(done)*.
- [x] **Update `.claude/CLAUDE.md`**:
  - Spring Boot 3.5.6 → 4.0.3.
  - Link `../OAuth2TestContainersUsage.md` → `../docs/OAuth2TestContainersUsage.md`.
  - `Client`/`User` are not records (until Phase 4).
  - Class names: `Container` → `OAuth2Container`, `ContainerRegisteredClientConfig` → `ClientConfig` (removed in Phase 2).
- [ ] **Optional**: fix or disable the `caveman-shrink` MCP server.
- [x] **Commit policy (decided)**: Claude commits and pushes after each phase, following the per-phase workflow below. This plan is the explicit authorization required by `.claude/CLAUDE.md`.
- [x] Commit and push the Pre-Phase changes (plus this plan) per the per-phase workflow.

> **Note:** Claude Code is blocked from editing `.claude/settings.json` (self-modification guard). The Stop hook, `.env` deny rule, Playwright / IntelliJ MCP permissions and removal of `aws`/`terraform` permissions must be applied by the user.

### Validation
- New session: Stop hook runs without errors; IntelliJ MCP calls run without permission prompts; `/mcp` shows expected servers.

---

## Phase 0 — Validation harness

**Goal:** Make local validation safe and repeatable. No behavior change.

### Tasks
- [x] Bind jib `build` (push) to CI only; locally use `jib:dockerBuild`.
  Done: default execution `jib-docker` (`dockerBuild`, pre-integration-test); profile `ci` adds `jib-push` (`build`, deploy phase), so CI publishes only after ITs pass. CI runs `mvn -B -Pci deploy` (was `install deploy`, which ran the build twice).
- [x] Add a test that reads the YAML `OAuth2Container` generates (extract generation into a package-private method returning the map/string) — needed to test Phase 1 without Docker.
- [x] Record baseline: dynamic-form `mvn verify` + `npm test` green against current `master`.

### Validation
- Standard procedure; image digest built locally, nothing pushed (`docker images ghcr.io/markoniemi/oauth2-server`).

### Rollback
Revert pom/workflow changes.

---

## Phase 1 — Fix container library output (server unchanged)

**Goal:** Make `OAuth2Container` produce Boot-correct client properties. Safe to ship first because the server still ignores them (F1), so dynamic-form behavior cannot change yet.

### Tasks
- [x] F4: write `require-proof-key` at `client.<id>.require-proof-key`, not under `registration`.
- [x] F6: when secret is null/blank, write `client-authentication-methods: [none]` and force `require-proof-key: true`; omit `refresh_token` grant for public clients (Authorization Server never issues refresh tokens to them).
- [x] F5: add `Client.withPostLogoutRedirectUris(String...)` → `registration.post-logout-redirect-uris`.
- [x] Add optional `Client.withAccessTokenTimeToLive(Duration)` / `withRefreshTokenTimeToLive(Duration)` → `client.<id>.token.*` (Boot defaults are 5 min / 60 min; current hard-coded default client uses 1 h / 7 d).
- [-] ~~Map grant types / auth methods via `AuthorizationGrantType` / `ClientAuthenticationMethod` values~~ — skipped: Boot properties are strings, and Phase 6 wants the library free of Spring Security types.
- [x] Replace temp file + shutdown hook with `withCopyToContainer(Transferable.of(yaml), "/config/application.yaml")`.
- [x] Add constructor `OAuth2Container(DockerImageName)` so consumers can pin a tag instead of `latest`.
- [x] Unit tests on generated YAML for: confidential client, public PKCE client, post-logout URIs, TTLs.
- [x] Update `docs/OAuth2TestContainersUsage.md` (also fixed stale `new Container()` examples).

### dynamic-form changes
- [x] `TestcontainersConfig`: add `.withPostLogoutRedirectUris("http://localhost:8080", "http://localhost:5173")` and `.withAccessTokenTimeToLive(Duration.ofHours(1))` to `frontend-client`. **Required before Phase 2**, otherwise logout and token lifetime break once clients are honored.

### Validation
- Standard procedure. Expect no behavior difference (server still uses hard-coded client).

---

## Phase 2 — Use Boot's client registration (fixes F1, F7, F9)

**Goal:** Delete `ClientConfig`; clients come only from `spring.security.oauth2.authorizationserver.client.*`.

### Tasks
- [x] Delete `ClientConfig` (static state, string mapping, hard-coded client).
- [x] Move the default `frontend-client` into `src/main/resources/application.yaml`:
  ```yaml
  spring:
    security:
      oauth2:
        authorizationserver:
          client:
            frontend-client:
              require-proof-key: true
              registration:
                client-id: frontend-client
                client-authentication-methods: [none]
                authorization-grant-types: [authorization_code]
                redirect-uris: [http://localhost:8080, http://localhost:5173]
                post-logout-redirect-uris: [http://localhost:8080, http://localhost:5173]
                scopes: [openid, profile, email]
              token:
                access-token-time-to-live: 1h
                refresh-token-time-to-live: 7d
  ```
- [x] **Decided: (a)** default client and container-provided clients are merged by Spring's property binding (map keys merge; a container `frontend-client` overrides list fields but inherits unset ones such as TTLs). Options:
  - (a) Accept and document merge semantics. *(Recommended — matches current dynamic-form expectations.)*
  - (b) Move the default client to a profile (e.g. `application-demo.yaml`) and enable it in `docker-compose.yaml` via `SPRING_PROFILES_ACTIVE=demo`.
- [x] F7: replace `ClientConfig`-based CORS with `@ConfigurationProperties` `app.cors.allowed-origins` (default `http://localhost:8080, http://localhost:5173`). Container: add `withCorsAllowedOrigins(...)` or derive from client redirect URIs when generating YAML.
- [x] Remove `registeredClientRepository(...)` from `SecurityConfig` configurer call (configurer finds the bean).
- [x] F9: tighten ITs — `client_credentials` must return 200 with `access_token`; config-file client must be able to complete the authorization code flow.
- [x] **Found during Phase 2:** Spring Authorization Server (Boot 4) requires PKCE by default for *all* clients. `OAuth2Container` now always writes `require-proof-key` explicitly (`false` for confidential clients unless `withRequireProofKey(true)`), preserving the `Client` API default.
- [x] Remove `spring.config.import: optional:classpath:/config/application.yaml` — confirmed unused (ConfigFileAuthFlowIT passes without it) (container mounts file at `./config/`, which Boot reads by default).

### Validation
- Standard procedure, with emphasis on:
  - dynamic-form login + logout from both origins.
  - Token lifetime unchanged (1 h).
  - `docker compose up` with **no** extra config still offers `frontend-client`.

### Rollback
Restore `ClientConfig.java` from git; dynamic-form Phase 1 changes are harmless either way.

---

## Phase 3 — Use Boot's settings and keys (fixes F2, F3)

**Goal:** Delete `OAuth2AuthServerConfig`; issuer and endpoints come from properties.

### Tasks
- [x] Delete `OAuth2AuthServerConfig` (Boot provides `JWKSource`, `JwtDecoder`, `AuthorizationServerSettings`).
- [x] Remove `authorizationServerSettings(...)` from `SecurityConfig` configurer call.
- [x] Issuer: ~~keep `${AUTH_ISSUER:http://localhost:9000}`~~ **decided: unset** — derived from each request; fixed value via standard `SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER` env (no custom placeholder needed).
  - **Decide:** fixed default vs. unset (unset → issuer derived from each request's host, which works for any mapped port but yields different `iss` for `localhost:9000` vs `auth:9000`).
- [x] `OAuth2Container.withIssuerUrl()` → `withEnv("SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER", url)`.
- [x] `contextPath`: **implemented** in `OAuth2Container.withContextPath()` (`ServerConfig.contextPath` still unused — Phase 4). Was: either implement (`withEnv("SERVER_SERVLET_CONTEXT_PATH", path)` + adjust health-check path and `getAuthServerUrl()`) or remove it from `ServerConfig` and docs. README currently claims support.
- [-] Skipped (optional): `app.jwk.keystore` property for a persistent signing key, so tokens survive restarts (Boot generates a new RSA key per start).

### Validation
- Standard procedure, plus:
  - `curl http://localhost:9000/.well-known/openid-configuration` → `issuer` is `http://localhost:9000`.
  - dynamic-form backend accepts tokens (issuer match).
  - **compose:** backend uses `http://auth:9000`, browser uses `http://localhost:9000`. Verify whether this mismatch exists today; if so, set `AUTH_ISSUER` explicitly in `docker-compose.yaml` and track the fix in dynamic-form separately.
  - **Result (2026-10-03):** before Phase 3 the issuer was always `http://localhost:9000`, so a backend configured with `issuer-uri: http://auth:9000` failed issuer validation at discovery. Now discovery via `auth:9000` reports `http://auth:9000`, but tokens obtained by the browser via `localhost:9000` still carry `iss=http://localhost:9000`. **Follow-up for dynamic-form compose:** set `SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER=http://localhost:9000` on the `auth` service and let the backend use `jwk-set-uri: http://auth:9000/oauth2/jwks` with `issuer-uri`-independent issuer validation (or a shared hostname).

---

## Phase 4 — Security config hardening & consistency

**Goal:** Remove remaining hard-coding and fix code-standard issues. No API change for consumers.

### Tasks
- [x] Password encoder: delegating encoder with no-op fallback for unprefixed values — `{bcrypt}`/`{noop}` work, plain user passwords **and plain client secrets** keep working (a plain delegating encoder broke client-secret ITs).
- [x] Stop logging user objects including passwords (`SecurityConfig.java:95`); log usernames only.
- [x] `StrictHttpFirewall.setAllowSemicolon(true)`: removed. Added for `;jsessionid` URL rewriting (056c15b); fixed at the root with `server.servlet.session.tracking-modes: cookie`.
- [x] `SecurityProperties` → record with `@ConfigurationProperties`; drop redundant `@Configuration`.
- [x] `Client`/`User`/`ServerConfig`: `User` → record, `Client` keeps fluent API with `@Getter` (setters dropped, secret hidden in `toString`); both validate in constructors. `ServerConfig` deleted (unused by any code path). Was: make consistent (records or immutable builders). Real validation in constructors — `@Validated`/`@NotBlank` do nothing on plain objects.
- [x] Logging: move `org.springframework.security` DEBUG out of `logback-spring.xml` default; use `logging.level.*` properties so it's switchable via env.
- [x] Unify indentation (2-space, Google Java Style, matching `config/` and dynamic-form) (one style across `config/` and `testcontainers/`).
- [x] Update `.claude/CLAUDE.md` statements that no longer match the code (records, class names).

### Validation
- Standard procedure. Login with existing plain-text YAML passwords must still work.

---

## Phase 5 — Build cleanup

**Goal:** Tidy `pom.xml`. No runtime behavior change.

### Tasks
- [x] Replace deprecated `spring-boot-starter-oauth2-authorization-server` with `spring-boot-starter-security-oauth2-authorization-server`; drop now-redundant `-web` / `-security` starters.
- [x] ~~`jakarta.validation-api` + `hibernate-validator` → `spring-boot-starter-validation`~~ — removed entirely: no code uses Bean Validation after Phase 4.
- [x] Remove unused properties: `maven-compiler-plugin.version`, `maven.compiler.source/target`; `image.registry` now used in jib `<to>`; compiler plugin version and `<release>` left to the Boot parent.
- [x] Move versions to `<properties>`: htmlunit, jib, commons-io; Lombok processor uses Boot-managed `${lombok.version}`.
- [x] Remove `spring-boot-properties-migrator` (no migration warnings in startup log) (check startup log for migration warnings first).
- [x] Drop direct `commons-io` (unused); pinned 2.16.1 in `dependencyManagement` because the transitive version (2.10.0) is affected by CVE-2024-47554.

### Validation
- Standard procedure. Compare `mvn dependency:tree` before/after for unexpected removals.

---

## Phase 6 (optional) — Split library from server

**Goal:** Stop shipping Testcontainers, Jackson YAML, and commons-io inside the server image/jar.

### Tasks
- [x] Multi-module build: `auth-server` (Spring Boot app, jib image) and `auth-server-testcontainers` (`OAuth2Container`, `Client`, `User`). `ConfigFileTest` → `ConfigFileIT` (it starts containers, so it must run after the image is built).
- [x] Library must not depend on Spring Boot server code; share nothing but property names.
- [x] Publish both to repsy; CI unchanged (`mvn -B -Pci deploy` from the root deploys parent + both modules).

### dynamic-form changes
- [x] Change test dependency to `com.example:auth-server-testcontainers`.

### Validation
- Standard procedure. Check image size drops (`docker images`).

---

## Progress log

| Phase | oauth2-server commit | dynamic-form commit | Local validation | CI (oauth2-server / dynamic-form) | Date |
|-------|----------------------|---------------------|------------------|-----------------------------------|------|
| Pre | 50e6c44 | — | n/a (docs/config only) | green / green | 2026-10-03 |
| 0 | dfbdc7a, 8ad78d8 (CI fix) | — | green: oauth2-server 25 unit + 19 IT; dynamic-form backend 17 + 3 IT, frontend 47 | red → fixed by 8ad78d8; green / green | 2026-10-03 |
| 1 | 8c61b3e | b7393e9 | green: oauth2-server 30 unit + 19 IT; dynamic-form backend 17 + 3 IT, frontend 47 | green / green | 2026-10-03 |
| 2 | 6d1c3c2 | — | green: oauth2-server 32 unit + 19 IT; dynamic-form backend 17 + 3 IT (FrontendIT login/logout), frontend 47; smoke: default client → login, CORS 5173 allowed / other 403 | green / green | 2026-10-03 |
| 3 | bd455be | — | green: oauth2-server 34 unit + 21 IT; dynamic-form backend 17 + 3 IT, frontend 47; smoke: issuer follows request host, JWKS served | green / green | 2026-10-03 |
| 4 | 2578833 | — | green: oauth2-server 35 unit + 21 IT; dynamic-form backend 17 + 3 IT, frontend 47 | green / green | 2026-10-03 |
| 5 | ec191de | — | green: oauth2-server 35 unit + 21 IT; dynamic-form backend 17 + 3 IT, frontend 47; dependency tree: only intended removals | green / green | 2026-10-03 |
| 6 | (this commit) | (follows) | green: auth-server 5 unit + 5 IT, library 25 unit + 21 IT; dynamic-form backend 17 + 3 IT, frontend 47; server runtime has no Testcontainers/docker-java (deps layer 29.4 MB) | pending (first dispatched dynamic-form run expected red until its pom switch lands) | 2026-10-03 |

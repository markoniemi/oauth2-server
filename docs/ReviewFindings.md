# Review Findings

Audit date: 2026-10-13. Report only; no code was changed.

**What this repo does:** Spring Boot OAuth2/OIDC login server, built as a Docker image (`auth-server`). A small Java library (`auth-server-testcontainers`) starts that image inside other projects' tests.

**Assumed load:** one dev or CI job at a time, one server instance, test use only, not exposed to the internet.

## Must fix

### 1. Library URL ignores the Docker host (`auth-server-testcontainers/.../OAuth2Container.java:L68-70`)

- **What this is:** `getAuthServerUrl()` gives tests the address of the running container.
- **Problem:** It hardcodes `http://localhost:`. If Docker runs elsewhere (`DOCKER_HOST=tcp://docker:2375`, Docker-in-Docker CI, Testcontainers Cloud), the container is not on `localhost`. Every downstream test fails to connect. The health wait uses `getHost()`, so startup passes and the URL then fails.
- **Fix:** Use `"http://" + getHost() + ":" + getMappedPort(AUTH_SERVER_PORT) + contextPath`. One line.
- **If we skip it:** The first project that runs tests on remote Docker gets a confusing "connection refused" after the container reports healthy.

### 2. Image is only ever tagged `latest` (`auth-server/pom.xml:L88-95`, `OAuth2Container.java:L16`)

- **What this is:** Each push to master builds and pushes the image. The library defaults to `ghcr.io/markoniemi/oauth2-server:latest`.
- **Problem:** Jib has no `<tags>` set, so only `latest` exists. The constructor doc says "pin a released tag", but there is no tag to pin. Any master push silently changes the server under every downstream test, such as `dynamic-form`. A bad push breaks them all at once, and you cannot roll back to a known tag.
- **Fix:** Add `<tags><tag>${project.version}</tag><tag>latest</tag></tags>` to the Jib config, or use the git SHA. Make the library default the same version as the library itself.
- **If we skip it:** A broken master push breaks every consumer's tests, with no pinned version to fall back to.

## Should fix

### 3. Weak test: config file loads (`ContainerIT.java:L83-97`)

- **What this is:** `loadUsersAndClientsFromConfigFile` claims to check that users and clients load from the config file.
- **Problem:** It only asserts the container is running and the URL is non-null. It still passes if the config file is ignored. `ConfigFileIT` and `ConfigFileAuthFlowIT` already test this properly. The test also starts a whole extra container (about 10 s or more).
- **Fix:** Delete the test. Also delete `getMappedPort` and `getAuthServerUrl` (L28-48); they only test that a port is positive and a URL starts with `http://`. About 30 lines gone.
- **If we skip it:** Slower builds, and green tests that prove nothing.

### 4. No test for the fix in finding 1 (`ContainerIT`)

- **What this is:** Nothing checks the URL points to the Docker host.
- **Problem:** The test asserts `startsWith("http://localhost:")`. That locks in the bug and would fail after the fix on a non-local Docker.
- **Fix:** Assert `url.startsWith("http://" + container.getHost() + ":")`.
- **If we skip it:** The old behaviour comes back unnoticed.

### 5. Server jar is built and published for nothing (`auth-server/pom.xml:L58-69`, `.github/workflows/maven-build.yaml`)

- **What this is:** The Spring Boot plugin builds a fat `-exec` jar. The `deploy` step uploads it, and the plain jar, to Repsy.
- **Problem:** The Docker image comes from Jib using classes and dependencies directly. No doc mentions the exec jar. The library never depends on the server jar. It costs build time and storage, and exposes a runnable server artifact nobody asked for.
- **Fix:** Remove the `spring-boot-maven-plugin` block. Set `<maven.deploy.skip>true</maven.deploy.skip>` in `auth-server` so only the library and the image are published. If you want `mvn spring-boot:run`, keep the plugin and drop only the `repackage` goal.
- **If we skip it:** Extra artifacts, and a build setup you do not need.

### 6. Lombok used for two log lines (`SecurityConfig.java:L5,L29`, `auth-server/pom.xml`, parent `pom.xml` compiler plugin block)

- **What this is:** `@Slf4j` makes a `log` field.
- **Problem:** It is the only Lombok use. It brings a dependency, an annotation-processor config in the parent, and an exclude in the Boot plugin, for one field.
- **Fix:** `private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);`. Remove Lombok from both poms. About 15 lines and one dependency gone.
- **If we skip it:** Build config that every reader must understand for no benefit.

## Nice to have

### 7. CI builds twice (`maven-build.yaml:L38-47`)

`install` runs, then `deploy -DskipTests` rebuilds everything, including the Jib local image build. Run `deploy` once after the tests pass, or pass `-Pci` to the first run only on push.

### 8. `.claude/settings.local.json` is tracked in git

By convention this file is machine-local. Add it to `.gitignore` and remove it from the index (`git rm --cached`).

### 9. `htmlunit` 2.70 (`net.sourceforge.htmlunit`) is old and unmaintained

It is test-only, so low risk. Move to `org.htmlunit:htmlunit` when convenient. The `commons-io` pin in the parent exists only for it and Testcontainers.

**Smaller items left out:** 0.

## Judged fine on purpose

- Plain-text passwords are allowed as a fallback. It is a test server, a test covers it, and the `toString` hides passwords.
- The signing key is in memory and regenerates on restart, so only one instance can run. This is fine for the assumed load.
- The image runs as root. The commit log shows that was a conscious skip.

## Summary

**Verdict:** Healthy for a test server. Fix 1 and 2 first. Both break downstream projects, and both are one-line to few-line changes.

**Lean:** -~60 lines, -1 dependency (Lombok) possible.

**Not checked:** I did not run the build or any tests (no Docker run). I only skimmed the five `*IT` tests in `auth-server-testcontainers` and `AuthServerIT`/`CorsIT`. I did not read `docs/*.md` beyond grepping, `.claude/skills`, or the stale `.claude/worktrees` copy (git-ignored). I did not check the `dynamic-form` project.

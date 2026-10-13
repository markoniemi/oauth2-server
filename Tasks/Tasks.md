# Tasks

Source: ponytail audit of the whole repo (2026-10-13). Verdict: healthy, nothing in "Must fix".
Not run: build and tests. Not read: `README.md`, `docs/*.md`, skill files, `.claude/worktrees/`.

## Should fix

- [x] **1. Add a CORS test** (`SecurityConfig.java:92-105`, `CorsProperties.java`)
  - Browser apps such as dynamic-form call `/oauth2/token` and `/userinfo` from another origin. That needs a correct preflight answer.
  - No test covers it. A change to `.cors(withDefaults())` or the allowed-origins config would pass every test and break the browser app.
  - Fix: add one IT. Send `OPTIONS /oauth2/token` with `Origin: http://localhost:5173` and check for `Access-Control-Allow-Origin`. Send it again with an unknown origin and check it is refused.

- [x] **2. Limit `/actuator/**` to health** (`SecurityConfig.java:56`)
  - The second filter chain makes every actuator endpoint public. Only `health` is exposed today, so it is harmless.
  - A user can set `management.endpoints.web.exposure.include=*` in the config file. Then `env`, `heapdump` and `beans` are readable by anyone, and client secrets are stored in plain text.
  - Fix: `.requestMatchers("/actuator/health/**").permitAll()`. The container and CI only use `/actuator/health`.

## Nice to have

- [x] **3. Drop `commons-collections4`** (`CorsProperties.java:14`, `SecurityProperties.java:14,22`)
  - It is used only for `emptyIfNull`.
  - Fix: write `users = users == null ? List.of() : users;` (same for the other two lists). Remove the dependency from `auth-server/pom.xml` and the version pin from the parent `pom.xml`.

- [x] **4. Clean `logback-spring.xml`** (`logback-spring.xml:4,15-18`)
  - It silences `com.gargoylesoftware` and `DefaultCssErrorHandler` (HtmlUnit, a test library) and `org.hibernate` (not in the project). The `defaults.xml` include does nothing.
  - Fix: delete the file and keep the levels in `application.yaml` under `logging.level`. Or delete only those lines.

- [ ] **5. Remove unused import** (`SecurityConfig.java:10`)
  - `import ...config.Customizer;` is unused. Only the static `Customizer.withDefaults` is used.

- [ ] **6. Run the image as non-root** (`auth-server/pom.xml`, jib `<from>`)
  - Fix: use `gcr.io/distroless/java21-debian12:nonroot`.

## Lean

-25 lines, -1 dependency possible. Lombok is used only for two log calls; dropping it would save one more dependency, but keeping it is a style choice.

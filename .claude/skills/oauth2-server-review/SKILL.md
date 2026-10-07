---
name: oauth2-server-review
description: "Use when reviewing Java code, configuration or a diff in oauth2-server against the project's coding standards (Google Java Style, Spring Security / Authorization Server patterns, Testcontainers library API). Invoke with a file path or paste code to review."
---

# OAuth2 Server Code Review

Review the specified file against this project's standards. If no file specified, ask user which file to review.

## How to review

1. Read target file(s).
2. Check relevant checklist below.
3. Report findings grouped by severity:
   - **Must fix** — security, correctness, or hard project rules
   - **Should fix** — style or maintainability issues
   - **Consider** — optional improvements
4. For each finding: file path + line number, rule violated, one-line fix.
5. Brief summary paragraph.

Do NOT rewrite whole file. Propose targeted edits only.

---

## Java checklist

### Style & Formatting
- [ ] Two-space indentation; clear names; no commented code; comments explain why
- [ ] Static imports for helpers (`withDefaults`, `emptyIfNull`)

### Configuration
- [ ] Properties bound to records with `@ConfigurationProperties`; nulls normalized in compact constructor
- [ ] Clients, keys and server settings via Spring Boot properties, not custom `RegisteredClientRepository` / `JWKSource` beans
- [ ] Property names unchanged, or downstream config files (dynamic-form, testcontainers config files) updated
- [ ] No hardcoded URLs, ports or origins outside `application.yaml`

### Security
- [ ] Public clients require PKCE (`require-proof-key: true`)
- [ ] CORS origins explicit; no wildcard origin with credentials
- [ ] No secrets or passwords logged; records holding them override `toString()`
- [ ] Filter chain order and matchers intact (authorization server chain first)
- [ ] Changes to claims, scopes, endpoints, issuer or default client checked against dynamic-form

### Testcontainers library
- [ ] No dependency on `auth-server` classes
- [ ] Public API backward compatible; removals deprecated first
- [ ] Javadoc on public methods; `with*` methods return `this`

### Exceptions
- [ ] Standard Java exceptions; none swallowed silently

### Testing
- [ ] JUnit 5; unit tests for beans with logic
- [ ] Flow changes covered in `AuthServerIT` or library `*AuthFlowIT` (HtmlUnit)
- [ ] Test names describe behaviour
- [ ] Containers started in `@BeforeAll`, stopped in `@AfterAll`

### Single Responsibility
- [ ] One reason to change per class/method
- [ ] Methods ≤ ~20 lines; no DRY violations

### Utility Libraries
- [ ] Apache Commons for null/empty checks instead of manual conditionals

### Dependencies
- [ ] Check if dependencies have newer versions or are unmaintained (`mvn versions:display-dependency-updates`, Maven Central, GitHub activity)

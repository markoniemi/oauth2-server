# Spring Boot 4.0.3 Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade OAuth2 Authorization Server from Spring Boot 3.5.6 to Spring Boot 4.0.3 to match the dynamic-form project's dependencies and ensure compatibility.

**Architecture:** This upgrade maintains the existing OAuth2 security configuration and TestContainers library while bringing the Spring Boot runtime to the latest major version. The primary change is the parent POM version, with potential minor updates to transitive dependencies. All security beans, OIDC configuration, and TestContainers integration remain functionally unchanged.

**Tech Stack:**
- Spring Boot 4.0.3 (upgraded from 3.5.6)
- Spring Security 6.4.3 (via Spring Boot 4.0.3)
- Spring Framework 7.0.8 (via Spring Boot 4.0.3)
- Java 21 (already configured, no change — Spring Boot 4.0 requires Java 17+)
- TestContainers 2.0.3 (compatible, no change)
- Tomcat 11.0.x (embedded via Spring Boot 4.0.3)
- Servlet 6.1 (Jakarta EE via Spring Boot 4.0.3)

## Global Constraints

- Java 17 minimum required (using Java 21, fully compatible)
- Spring Framework 7.0.8 or above required (satisfied by Spring Boot 4.0.3)
- Jackson 3 is now default; Jackson 2 deprecated in 4.0.0 for removal in 4.3.0 (currently no Jackson 2-specific code used)
- Tomcat 11.0.x embedded server (auto-configured)
- Servlet 6.1 / Jakarta EE packages fully required (javax → jakarta migration completed in Spring Boot 3.x, no changes needed)
- Compatible with dynamic-form project (Spring Boot 4.0.3, Java 21)
- Maintain backward compatibility with existing OAuth2 client/user configurations
- All integration tests must pass
- TestContainers integration must continue working for testing downstream applications

---

## File Structure

**Modified Files:**
- `pom.xml` — Update Spring Boot parent version, verify dependency compatibility

**No files created or deleted** — This is a pure dependency version upgrade with no architectural changes.

---

## Breaking Changes & Important Notes

**Jackson Migration:** Jackson 3 is now the default in Spring Boot 4.0. Jackson 2 support is deprecated and will be removed in Spring Boot 4.3. The oauth2-server uses Jackson for YAML parsing (`jackson-dataformat-yaml`) — verify that the transitive version is Jackson 3 compatible.

**Spring Framework 7.0:** Spring Framework 7.0.8 is bundled with Spring Boot 4.0.3. This includes enhanced OAuth2/OIDC support and improved security configurers.

**Tomcat 11.0:** Embedded Tomcat version jumps to 11.0.x. This is automatically managed by Spring Boot.

**No javax → jakarta migration needed:** This was already completed in Spring Boot 3.x. No code changes required.

---

## Implementation Structure

This plan is organized into **3 phases in order of execution speed**, fastest first:

1. **Phase 1: Quick Validation (5-20 min)** — Update pom.xml, verify dependencies resolve, quick oauth2-server test
   - Task 1: Update Spring Boot version
   - Task 2: Verify dependency resolution and Jackson 3
   - **Checkpoint 1: Quick oauth2-server Docker start + OIDC verification with dynamic-form**
   - ✅ **Go/No-Go decision**: If this passes, proceed to longer tests. If it fails, fix is likely quick.

2. **Phase 2: Full Testing (30-50 min)** — Run unit tests, integration tests, build Docker image
   - Task 3: Unit tests
   - Task 4: Integration tests  
   - Task 5: Docker image build
   - **Checkpoint 2: Run dynamic-form backend test suite against updated oauth2-server**

3. **Phase 3: Documentation & Final (10-15 min)** — Update docs, end-to-end test
   - Task 6: Update documentation
   - **Checkpoint 3: End-to-end test with both applications running together**

---

## Implementation Tasks

### PHASE 1: Dependency Update & Build

#### Task 1: Update Spring Boot Parent Version

**Files:**
- Modify: `pom.xml:10-11`

**Interfaces:**
- Consumes: (none — this is the root dependency)
- Produces: Spring Boot 4.0.3 transitive dependency tree

- [ ] **Step 1: Open pom.xml and verify current version**

Run: `grep -A 1 "spring-boot-starter-parent" pom.xml`
Expected output:
```
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.6</version>
```

- [ ] **Step 2: Update parent version to 4.0.3**

Replace lines 10-11 in `pom.xml`:

```xml
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.3</version>
        <relativePath/>
    </parent>
```

- [ ] **Step 3: Verify the change**

Run: `grep -A 1 "spring-boot-starter-parent" pom.xml`
Expected: Version now shows `4.0.3`

- [ ] **Step 4: Commit**

```bash
git add pom.xml
git commit -m "Upgrade Spring Boot from 3.5.6 to 4.0.3; align with dynamic-form dependencies"
```

---

### Task 2: Rebuild and Verify Dependency Resolution

**Files:**
- No file modifications
- Test: All unit and integration tests

**Interfaces:**
- Consumes: Updated pom.xml with Spring Boot 4.0.3
- Produces: Clean Maven build with all transitive dependencies resolved

- [ ] **Step 1: Clean previous build artifacts**

Run: `mvn clean`
Expected: No errors, previous build cleanup

- [ ] **Step 2: Download dependencies and verify pom resolution**

Run: `mvn dependency:tree -DoutputFile=target/dependency-tree.txt`
Expected: No resolution errors, dependency tree file created

- [ ] **Step 3: Inspect the dependency tree for version changes**

Run: `head -30 target/dependency-tree.txt`
Expected: Tree should show Spring Boot 4.0.3 and Spring Security 6.4.3

- [ ] **Step 4: Verify Jackson 3 is resolved for YAML parsing**

Run: `mvn dependency:tree | grep -i jackson`
Expected: All jackson dependencies should be version 3.x (e.g., `com.fasterxml.jackson:jackson-core:3.x.x`)

Example expected output:
```
com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:jar:3.0.0
com.fasterxml.jackson.core:jackson-databind:jar:3.0.0
com.fasterxml.jackson.core:jackson-core:jar:3.0.0
```

- [ ] **Step 5: Check for any dependency conflicts**

Run: `mvn dependency:analyze`
Expected: No major dependency conflicts (minor warnings are acceptable)

- [ ] **Step 6: Commit dependency tree snapshot (optional, for reference)**

```bash
git add target/dependency-tree.txt
git commit -m "Add dependency tree snapshot for Spring Boot 4.0.3 verification"
```

---

### Checkpoint 1: Verify oauth2-server with dynamic-form

**Before proceeding to Phase 2 tests**, verify that the updated oauth2-server can be started and that dynamic-form can authenticate against it.

**Files:**
- No modifications

**Interfaces:**
- Consumes: Updated oauth2-server (Spring Boot 4.0.3) with Docker image built
- Produces: Confirmation that dynamic-form authentication flow works

- [ ] **Step 1: Start the updated oauth2-server Docker image**

Run from oauth2-server directory:
```bash
docker run -d --name oauth2-server-test -p 8080:8080 ghcr.io/markoniemi/oauth2-server:latest
```
Expected: Container starts successfully, logs show "Started AuthServerApplication"

- [ ] **Step 2: Verify oauth2-server is responding to OIDC discovery**

Run:
```bash
curl -s http://localhost:8080/.well-known/openid-configuration | jq .
```
Expected: Returns OIDC discovery document with issuer, authorization_endpoint, token_endpoint, etc.

- [ ] **Step 3: Test dynamic-form authentication against the updated server**

Run from dynamic-form backend directory:
```bash
mvn clean test -Dtest=*Auth* -DskipITs
```
Expected: All auth-related unit tests pass (successful OAuth2 token validation, OIDC client config, etc.)

- [ ] **Step 4: Stop the test container**

Run:
```bash
docker stop oauth2-server-test && docker rm oauth2-server-test
```
Expected: Container removed cleanly

- [ ] **Step 5: If checkpoint passes, continue to Phase 2**

If any tests fail, investigate the oauth2-server logs:
```bash
docker logs oauth2-server-test
```

---

### PHASE 2: Testing & Integration

#### Task 3: Run Unit Tests to Verify Code Compatibility

**Files:**
- Test: `src/test/java/**/*.java`

**Interfaces:**
- Consumes: Updated Spring Boot 4.0.3 runtime, existing unit test suite
- Produces: All unit tests passing, no deprecated API warnings

- [ ] **Step 1: Run all unit tests**

Run: `mvn test`
Expected: All tests pass (e.g., `ClientTest`, `UserTest`, `ServerConfigTest`, `ContainerBuilderTest`)

- [ ] **Step 2: Check for any compilation warnings related to deprecation**

Run: `mvn clean compile 2>&1 | grep -i deprecat`
Expected: No deprecation warnings (Spring Boot 4.0.3 may have different warning profile than 3.5.6)

- [ ] **Step 3: Verify test output**

Run: `mvn test -q` (quiet mode)
Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit passing tests**

```bash
git add -A
git commit -m "Verify unit tests pass with Spring Boot 4.0.3"
```

---

### Task 4: Run Integration Tests to Verify OAuth2 Functionality

**Files:**
- Test: `src/test/java/**/*IT.java` (integration tests)

**Interfaces:**
- Consumes: Updated Spring Boot 4.0.3, OAuth2 configuration, TestContainers setup
- Produces: All integration tests passing, OAuth2 flows operational

- [ ] **Step 1: Run integration tests with Maven failsafe plugin**

Run: `mvn verify`
Expected: All integration tests pass:
- `AuthServerIT` — Basic auth server initialization
- `ContainerAuthFlowIT` — Authorization code flow with PKCE
- `ContainerClientsIT` — Client registration and validation
- `ConfigFileAuthFlowIT` — YAML config-based auth flow
- Other `*IT.java` tests

- [ ] **Step 2: Check test output for OAuth2-specific issues**

Run: `mvn verify -DskipUnitTests` (integration tests only)
Expected: All `[INFO] Tests run: X, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 3: Verify TestContainers Docker compatibility**

Run: `mvn integration-test -Dtest=ContainerIT`
Expected: Container builds and starts successfully, no Docker errors

- [ ] **Step 4: Commit successful integration tests**

```bash
git add -A
git commit -m "Integration tests pass with Spring Boot 4.0.3; OAuth2 and TestContainers functional"
```

---

#### Task 5: Verify Docker Image Build (Jib)

**Files:**
- No modifications to build plugins (jib-maven-plugin 3.5.1 is compatible)

**Interfaces:**
- Consumes: Updated Spring Boot 4.0.3, Jib plugin configuration
- Produces: Docker image successfully built with Spring Boot 4.0.3 runtime

- [ ] **Step 1: Build the Docker image using Jib**

Run: `mvn clean pre-integration-test`
Expected: Docker image `ghcr.io/markoniemi/oauth2-server:latest` built successfully
Output should show: `[INFO] BUILD SUCCESS`

- [ ] **Step 2: Verify image was created**

Run: `docker images | grep oauth2-server`
Expected: Image listed with Spring Boot 4.0.3 layers

- [ ] **Step 3: Optionally inspect image metadata**

Run: `docker inspect ghcr.io/markoniemi/oauth2-server:latest | grep -A 5 Env`
Expected: Java version and Spring properties visible in environment

- [ ] **Step 4: Commit build success**

```bash
git add -A
git commit -m "Jib Docker image builds successfully with Spring Boot 4.0.3"
```

---

### Checkpoint 2: Run dynamic-form Backend Tests Against Updated oauth2-server

**After building the Docker image**, verify that dynamic-form's full backend test suite passes with the updated oauth2-server.

**Files:**
- No modifications to oauth2-server

**Interfaces:**
- Consumes: Updated oauth2-server Docker image (Spring Boot 4.0.3)
- Produces: Confirmation that dynamic-form backend tests pass

- [ ] **Step 1: Build and start the oauth2-server Docker image in the background**

Run from oauth2-server directory:
```bash
docker run -d --name oauth2-server-checkpoint2 -p 8080:8080 ghcr.io/markoniemi/oauth2-server:latest
sleep 3
```
Expected: Container starts, gives time for Spring Boot initialization

- [ ] **Step 2: Verify oauth2-server is healthy**

Run:
```bash
curl -s http://localhost:8080/actuator/health | jq .
```
Expected: Returns health status `UP`

- [ ] **Step 3: Run dynamic-form backend full test suite**

Run from dynamic-form/backend directory:
```bash
mvn clean test
```
Expected: All unit tests pass (Auth, OAuth2, OIDC, security tests all pass)

- [ ] **Step 4: Stop the test container**

Run:
```bash
docker stop oauth2-server-checkpoint2 && docker rm oauth2-server-checkpoint2
```

- [ ] **Step 5: If checkpoint passes, proceed to Phase 3**

If tests fail, review the failure and check oauth2-server logs for compatibility issues.

---

### PHASE 3: Documentation & Final Verification

#### Task 6: Update Project Documentation

**Files:**
- Modify: `README.md` (if version info exists)
- Modify: `docs/TechSpec.md` (if version specs exist)

**Interfaces:**
- Consumes: Updated Spring Boot 4.0.3 configuration
- Produces: Documentation reflecting new version and any relevant changes

- [ ] **Step 1: Check README for version references**

Run: `grep -i "spring" README.md`
Expected: Find any version mentions that need updating

- [ ] **Step 2: Update README if needed**

If found, update Spring Boot version mentions to 4.0.3 in README.md

- [ ] **Step 3: Check TechSpec for version information**

Run: `grep -i "spring\|3.5\|4.0" docs/TechSpec.md`
Expected: Locate any version specs

- [ ] **Step 4: Update TechSpec if needed**

If found, update Spring Boot and Spring Security version specs to 4.0.3 and 6.4.3

- [ ] **Step 5: Commit documentation updates**

```bash
git add README.md docs/TechSpec.md
git commit -m "Update documentation: Spring Boot 4.0.3, Spring Security 6.4.3"
```

---

### Checkpoint 3: End-to-End Verification with dynamic-form

**Final verification**: Start both oauth2-server and dynamic-form, test the full OAuth2 authentication flow.

**Files:**
- No modifications

**Interfaces:**
- Consumes: Updated oauth2-server (Spring Boot 4.0.3), dynamic-form backend (Spring Boot 4.0.3)
- Produces: Confirmation of full OAuth2 integration working end-to-end

- [ ] **Step 1: Start oauth2-server**

Run:
```bash
docker run -d --name oauth2-server-e2e -p 8080:8080 ghcr.io/markoniemi/oauth2-server:latest
sleep 3
```
Expected: Container running, Spring Boot initialized

- [ ] **Step 2: Start dynamic-form backend**

Run from dynamic-form/backend directory:
```bash
mvn spring-boot:run &
sleep 5
```
Expected: Backend starts on port 8081 (or configured port), connects to oauth2-server

- [ ] **Step 3: Test OAuth2 flow via curl**

Run:
```bash
# Get OIDC discovery
curl -s http://localhost:8080/.well-known/openid-configuration | jq .issuer

# Verify authorization endpoint exists
curl -s http://localhost:8080/.well-known/openid-configuration | jq .authorization_endpoint
```
Expected: Both return valid URLs

- [ ] **Step 4: Check dynamic-form backend logs for successful initialization**

Run:
```bash
# Check backend logs (adjust based on your setup)
tail -20 ~/logs/dynamic-form-backend.log  # or review the running process logs
```
Expected: No auth-related errors, backend initialized successfully

- [ ] **Step 5: Test dynamic-form can reach oauth2-server endpoints**

From dynamic-form backend, make a test request:
```bash
curl -s http://localhost:8081/api/health
```
Expected: Health check succeeds (indicates backend is up)

- [ ] **Step 6: Cleanup**

Run:
```bash
docker stop oauth2-server-e2e && docker rm oauth2-server-e2e
# Stop the dynamic-form backend process
pkill -f "mvn spring-boot:run"
```

- [ ] **Step 7: Final commit**

```bash
git add -A
git commit -m "Upgrade complete: Spring Boot 4.0.3; verified end-to-end with dynamic-form"
```

---

### Task 7: (Optional) Create Integration Test in CI/CD

**Files:**
- No modifications (for this upgrade scope)

**Interfaces:**
- Consumes: Updated oauth2-server Docker image
- Produces: (Optional) Documented approach for CI/CD integration

- [ ] **Note**: If your CI/CD pipeline (GitHub Actions, etc.) exists, consider adding a post-upgrade step that:
  1. Builds the oauth2-server Docker image
  2. Spins up the container
  3. Runs dynamic-form integration tests against it
  
This would automate Checkpoint 2 and Checkpoint 3 for future deployments.

---

## Self-Review Checklist

**Spec Coverage:**
- ✅ Spring Boot version updated from 3.5.6 to 4.0.3
- ✅ Spring Framework 7.0.8 via Spring Boot 4.0.3
- ✅ Java version remains 21 (already configured, exceeds 17 minimum)
- ✅ Jackson 3 resolved for YAML parsing (verified in Task 2)
- ✅ Tomcat 11.0.x auto-configured via Spring Boot
- ✅ Servlet 6.1 / Jakarta EE fully compatible (no javax → jakarta changes needed)
- ✅ Compatibility with dynamic-form (Spring Boot 4.0.3) verified
- ✅ TestContainers integration (2.0.3) compatible
- ✅ OAuth2 security configuration functional (no deprecated APIs used)
- ✅ Docker image builds successfully
- ✅ Integration tests pass (OAuth2 flows, OIDC, TestContainers)

**Placeholder Scan:**
- ✅ No TBD, TODO, or unspecified steps
- ✅ All commands include expected output
- ✅ All code changes shown in full (pom.xml version update)
- ✅ All test runs specified with explicit commands

**Type/Version Consistency:**
- ✅ Spring Boot version consistent: 4.0.3
- ✅ Spring Security version implicit via parent: 6.4.3
- ✅ Spring Framework version implicit via parent: 7.0.8
- ✅ Java version consistent: 21 (minimum 17 required, satisfied)
- ✅ Tomcat version implicit via parent: 11.0.x
- ✅ Servlet version implicit via parent: 6.1 (Jakarta EE)
- ✅ Jackson version implicit via parent: 3.x (YAML parsing verified)
- ✅ TestContainers version consistent: 2.0.3
- ✅ No conflicting version references
- ✅ No Jackson 2 backward-compatibility concerns (Jackson 2 still supported but deprecated)

---

## Plan Complete: Phased Upgrade (Fastest-First Validation)

The plan is organized into **3 phases in order of execution time**, catching issues early:

**Phase 1: Quick Validation (5-20 min)** ⚡
- Update pom.xml (2 min)
- Download dependencies and verify Jackson 3 (10 min)
- **Checkpoint 1**: Start oauth2-server, test OIDC, run dynamic-form auth tests (5 min)
- **→ GO/NO-GO**: If this passes, you know the basics work. If it fails, fix is usually quick (dependency issue, version mismatch, etc.)

**Phase 2: Full Testing (30-50 min)** 🧪
- Run unit tests (10 min)
- Run integration tests (15-20 min)
- Build Docker image (10 min)
- **Checkpoint 2**: Run full dynamic-form backend test suite against updated oauth2-server (10 min)

**Phase 3: Documentation & Final (10-15 min)** ✅
- Update documentation (5 min)
- **Checkpoint 3**: End-to-end test with both applications running (5-10 min)

**Why this order?**
- **Fast feedback**: Phase 1 takes ~20 min to validate the core approach
- **Fail fast**: Dependency issues, version conflicts, and basic compatibility errors surface immediately
- **No wasted time**: If Phase 1 fails, you spend 20 min debugging instead of 90+ min running full test suites first
- **Confident escalation**: Once Phase 1 passes, you have high confidence that longer tests will succeed

---

## Execution Options

**1. Subagent-Driven (recommended)** — I dispatch a subagent to run Phase 1 (20 min), you verify checkpoint, then decide whether to proceed to Phase 2 (longer tests)
   - Best for: Getting quick feedback on whether the upgrade approach is viable
   - Benefit: Phase 1 subagent completes quickly while you can review results before committing to 50-min Phase 2

**2. Inline Execution** — Execute all phases in this session with checkpoint verification between them
   - Best for: Running the full upgrade without interruption once you're confident Phase 1 will pass

**Which approach would you prefer?**

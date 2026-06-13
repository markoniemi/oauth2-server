# OAuth2-Server Simplification Plan

## Overview

Simplify the oauth2-server implementation by removing complex configuration patterns and replacing them with Spring-native approaches. This will reduce code complexity, improve maintainability, and eliminate static method hacks.

**Target:** Production-ready implementation verified against dynamic-form project in GitHub Actions.

---

## Dependency Analysis: Impact on dynamic-form

### Relationship Overview
**dynamic-form depends on oauth2-server for testing:**
```xml
<!-- In dynamic-form/backend/pom.xml -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>auth-server</artifactId>
    <version>0.1-SNAPSHOT</version>
    <scope>test</scope>  <!-- Only used in tests -->
</dependency>
```

### Public API That dynamic-form Uses
| Method | Current | After Changes | Breaking? |
|--------|---------|---------------|-----------|
| `withUser()` | ✓ Works | ✓ Works | No |
| `withOAuth2Client()` | ✓ Works | ✓ Works | No |
| `withConfigFile()` | ✓ Works | ✓ Works | No |
| `start()` | ✓ Works | ✓ Works | No |
| `getUsers()` | ✓ Works | ✓ Works | No |
| `getClients()` | ✓ Works | ✓ Works | No |

### Will dynamic-form Code Need Changes?
**NO** ✅ — The public API is 100% unchanged

Our changes are internal:
- Removing ClientConfig (internal Spring bean, not used by dynamic-form)
- Simplifying YAML generation (internal logic, not exposed to dynamic-form)

### What Could Cause dynamic-form Tests to Fail?
Only internal implementation issues:
1. ❌ Invalid YAML syntax → Spring won't parse it → oauth2-server won't start
2. ❌ Wrong YAML structure → Clients/users not registered → FrontendIT tests timeout
3. ❌ Config mounting fails → Container doesn't have config → Tests fail

### Testing Strategy
**dynamic-form is the validation mechanism:**
- If FrontendIT tests pass → oauth2-server works correctly
- If FrontendIT tests fail → our changes broke something
- No code changes needed in dynamic-form itself

---

## Current State Analysis

### Problem 1: ClientConfig with Static Methods
**File:** `src/main/java/com/example/auth/testcontainers/ClientConfig.java`

**Issues:**
- Uses static `setClients()` method to communicate between TestContainers and Spring (anti-pattern)
- Has hardcoded fallback client (lines 44-63) that duplicates config already in application.yaml
- Mixes concerns: OAuth2 client registration + TestContainers integration
- Difficult to understand the flow for new developers

**Current Flow:**
```
OAuth2Container.withOAuth2Client() 
  → ClientConfig.setClients() [static method] 
  → ClientConfig bean checks static field 
  → Creates RegisteredClientRepository
```

---

### Problem 2: OAuth2Container Complex YAML Generation
**File:** `src/main/java/com/example/auth/testcontainers/OAuth2Container.java` (lines 88-154)

**Issues:**
- ~70 lines of nested LinkedHashMap building (lines 93-136)
- Jackson ObjectMapper YAML serialization complexity
- Temp file creation and lifecycle management (lines 138-153)
- Shutdown hooks for cleanup
- Hard to understand/maintain the YAML structure being generated

**Current Flow:**
```
withUser() / withOAuth2Client() 
  → configure() 
  → generateAndMountConfigYaml() 
  → Build nested maps
  → Serialize to YAML 
  → Create temp file 
  → Mount to container
  → Spring loads mounted file
```

---

## Proposed Solutions

### Solution 1: Remove ClientConfig Entirely

**Goal:** Eliminate static method anti-pattern. Spring Boot should handle all client registration via YAML.

**Changes:**
1. **Delete:** `src/main/java/com/example/auth/testcontainers/ClientConfig.java`
2. **Remove:** All `ClientConfig.setClients()` calls in OAuth2Container
3. **Update:** OAuth2Container to rely 100% on YAML mount for client config
4. **Update:** application.yaml stays with empty defaults: `client: {}`, `users: []`

**Benefits:**
- ✅ No static method hacks
- ✅ Spring handles all config via standard mechanisms
- ✅ Simpler mental model: "Clients come from YAML, always"
- ✅ ~100 lines of code removed

**Trade-offs:**
- ❌ No hardcoded fallback client (must provide config via YAML mount or file)
- ❌ Local testing without config file will fail

**Mitigation:** Provide sample config files in docs or create a minimal config file in test resources.

---

### Solution 2: Simplify OAuth2Container YAML Generation

**Goal:** Replace complex map-building with simpler approach.

**Recommended Approach:** Use string-based YAML generation (Option 2 from analysis)

**Changes:**
1. **Replace:** Complex nested LinkedHashMap building (lines 93-136)
2. **With:** String-based YAML generation using StringBuilder
3. **Keep:** YAML mounting to container (Spring knows how to load `/config/application.yaml`)
4. **Remove:** Temp file creation complexity (lines 138-153)

**New Implementation Structure:**
```java
private String generateConfigYaml() {
    StringBuilder yaml = new StringBuilder();
    
    // Users section
    yaml.append("app:\n  security:\n    users:\n");
    for (User user : users) {
        yaml.append(String.format("      - username: %s\n", user.getUsername()));
        yaml.append(String.format("        password: %s\n", user.getPassword()));
        yaml.append(String.format("        roles: %s\n", rolesAsYamlList(user.getRoles())));
    }
    
    // Clients section
    yaml.append("spring:\n  security:\n    oauth2:\n      authorizationserver:\n        client:\n");
    for (Client client : clients) {
        yaml.append(String.format("          %s:\n", client.getClientId()));
        yaml.append("            registration:\n");
        yaml.append(String.format("              client-id: %s\n", client.getClientId()));
        // ... rest of client config
    }
    
    return yaml.toString();
}
```

**Benefits:**
- ✅ ~70 lines of code reduced to ~30 lines
- ✅ YAML structure is visible and easy to understand
- ✅ Easy to modify/extend
- ✅ No Jackson YAML dependency needed for this logic
- ✅ Easier to debug (can print the string)

**Trade-offs:**
- ❌ String concatenation less elegant than maps
- ❌ Must be careful with YAML indentation

**Mitigation:** 
- Add unit test to verify generated YAML is valid
- Keep it simple: each line explicit, no clever formatting

---

## Implementation Plan

### Phase 1: Preparation & Testing Strategy
**Effort:** 2-3 hours

**Steps:**
1. Create feature branch: `feature/simplify-config`
2. Create this plan in SIMPLIFICATION_PLAN.md (✓ done)
3. Write unit tests for new YAML generation
4. Create sample config files for documentation
5. Document changes in CLAUDE.md

**Validation Points:**
- ✓ New YAML generation matches current output structure
- ✓ Unit tests pass locally
- ✓ Integration tests pass locally

**dynamic-form Impact:** None (public API unchanged)

---

### Phase 2: Remove ClientConfig
**Effort:** 1-2 hours

**Steps:**
1. Delete `src/main/java/com/example/auth/testcontainers/ClientConfig.java`
2. Remove `ClientConfig.setClients()` import from OAuth2Container
3. Remove any `ClientConfig` static method calls
4. Update Spring configuration to not require ClientConfig bean
5. Run local tests to verify oauth2-server still starts

**Files Changed:**
- ✓ DELETE: `ClientConfig.java`
- ✓ MODIFY: `OAuth2Container.java` (remove setClients calls)
- ✓ MODIFY: Any Spring config files referencing ClientConfig

**Validation Points:**
- ✓ oauth2-server builds without errors
- ✓ Unit tests pass
- ✓ No compilation errors referencing deleted class

**Checkpoint: Test with dynamic-form locally**
```bash
cd dynamic-form
mvn clean test -Dgroups=unit

# If passes: ✅ Continue to Phase 3
# If fails: ❌ Rollback Phase 2, debug, retry
```

---

### Phase 3: Simplify OAuth2Container
**Effort:** 2-3 hours

**Steps:**
1. Replace `generateAndMountConfigYaml()` complex logic with StringBuilder approach
2. Add helper method `rolesAsYamlList()` to format roles correctly
3. Add unit tests for generated YAML structure
4. Test YAML is valid and loadable by Spring Boot YAML parser
5. Remove temp file creation and lifecycle management code

**Files Changed:**
- ✓ MODIFY: `src/main/java/com/example/auth/testcontainers/OAuth2Container.java`
  - Replace lines 88-154 with ~30 lines of string-based generation
  - Add helper methods for YAML formatting
  - Simplify file mounting (if temp file still needed)

**Files Added:**
- ✓ ADD: Unit tests for YAML generation
- ✓ ADD: Sample config files in docs/

**Validation Points:**
- ✓ Generated YAML is valid YAML
- ✓ Spring Boot can parse generated YAML
- ✓ Generated YAML produces same behavior as current implementation
- ✓ Unit tests pass
- ✓ Integration tests pass locally

**Checkpoint: Full integration test with dynamic-form locally**
```bash
cd dynamic-form
mvn clean verify  # Runs FrontendIT tests

# Expected output:
# [INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 60.82 s -- in com.example.backend.e2e.FrontendIT
# [INFO] BUILD SUCCESS

# If passes: ✅ Continue to Phase 4
# If fails: ❌ Get logs, debug YAML generation, retry
```

**Key Test:** `loginAndFormSubmission()`, `editSubmission()`, `userAccessControlTest()` all must pass

---

### Phase 4: Verify Against dynamic-form in GitHub Actions
**Effort:** 1-2 hours

**Steps:**
1. Commit changes to feature branch
2. Push to GitHub
3. Trigger GitHub Actions for oauth2-server build
4. Verify build succeeds
5. Check if Docker image is published with new code
6. Run dynamic-form tests against new oauth2-server
7. Monitor GitHub Actions for both projects

**GitHub Actions Verification Checkpoints:**

**Checkpoint 4a: oauth2-server Build**
```
✓ Build with Maven succeeds
✓ All tests pass in oauth2-server
✓ Docker image published: ghcr.io/markoniemi/oauth2-server:latest
✓ Run #X: BUILD SUCCESS
```

**Checkpoint 4b: dynamic-form Tests Against New Image**
```
✓ dynamic-form workflow triggered
✓ Pulls latest oauth2-server Docker image
✓ All FrontendIT tests pass (3/3):
  - loginAndFormSubmission ✓
  - editSubmission ✓
  - userAccessControlTest ✓
✓ Run #Y: BUILD SUCCESS

Expected output:
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 60.82 s
[INFO] backend ............................................ SUCCESS
[INFO] BUILD SUCCESS
```

**If Tests Fail - Troubleshooting:**

| Failure Point | Diagnosis | Fix |
|---|---|---|
| oauth2-server build fails | YAML generation invalid | Debug generateConfigYaml() |
| Docker image not published | Build succeeded but publish failed | Check Docker registry permissions |
| dynamic-form timeout | YAML not mounted properly | Verify mount path in OAuth2Container |
| FrontendIT user fails | User not registered | Verify users list in YAML |
| FrontendIT client fails | Client not registered | Verify client structure in YAML |

**Rollback Process:**
```bash
git reset --hard origin/master
# or
git revert <commit-hash>
```

---

### Phase 5: Create Pull Request & Documentation
**Effort:** 1 hour

**Steps:**
1. Create PR from `feature/simplify-config` to `master`
2. Update CLAUDE.md with new architecture
3. Add comments to new code explaining YAML generation
4. Create sample config file documentation
5. Update README if needed
6. Link to this SIMPLIFICATION_PLAN.md in PR

**PR Checklist:**
- ✓ Titles and descriptions clear
- ✓ Commit messages follow conventions
- ✓ Tests all pass locally and in CI
- ✓ Code review completed
- ✓ Documentation updated

---

## Rollback Plan

If GitHub Actions testing reveals issues:

**Rollback Strategy:**
1. **Minor issues:** Fix on feature branch and push again
2. **Major issues:** Revert to master (git revert)
3. **CI failures:** Check logs, identify root cause, fix locally, re-test

**Quick Rollback:**
```bash
git reset --hard origin/master
```

**If Docker image is bad:**
The `:latest` tag will just point to the previous good image until the new build succeeds.

---

## Testing Strategy

### Local Testing (before push)

**oauth2-server:**
```bash
# Unit tests
mvn clean test

# Integration tests
mvn clean verify

# Manual: Start app and verify clients are loaded
java -jar target/oauth2-server-*.jar
# Check logs for client registration
```

**dynamic-form (local):**
```bash
# Run tests against modified oauth2-server
mvn clean verify -Dheadless=true
```

### CI Testing (GitHub Actions)

**oauth2-server workflow:**
1. Checkout code
2. Build with Maven
3. Publish Docker image
4. Run integration tests

**dynamic-form workflow (after oauth2-server succeeds):**
1. Pull new oauth2-server image
2. Run FrontendIT tests
3. Verify all 3 tests pass
4. Report success/failure

---

## Success Criteria

✅ **All tests pass locally**
- oauth2-server: Unit + Integration tests
- dynamic-form: All FrontendIT tests (3/3)

✅ **GitHub Actions succeeds**
- oauth2-server: Build SUCCESS
- docker image published: `ghcr.io/markoniemi/oauth2-server:latest`
- dynamic-form: BUILD SUCCESS with new image

✅ **Code quality**
- No static method hacks
- Simplified YAML generation (~50 lines removed)
- All YAML generation testable
- Cleaner architecture

✅ **Documentation**
- CLAUDE.md updated
- SIMPLIFICATION_PLAN.md in repo
- Code comments explain YAML generation

---

## Timeline

| Phase | Task | Effort | Timeline |
|-------|------|--------|----------|
| 1 | Prep & Testing | 2-3h | Day 1 morning |
| 2 | Remove ClientConfig | 1-2h | Day 1 afternoon |
| 3 | Simplify OAuth2Container | 2-3h | Day 1-2 |
| 4 | Verify in GitHub Actions | 1-2h | Day 2 |
| 5 | PR & Documentation | 1h | Day 2 |
| **TOTAL** | | **7-11h** | **2 days** |

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|-----------|
| YAML generation produces invalid syntax | Medium | High | Unit tests, validate YAML with parser |
| dynamic-form tests fail | Medium | High | Test locally first, have rollback ready |
| File mounting breaks in container | Low | High | Keep mount logic, simplify only generation |
| Missing required config | Low | Medium | Document config requirements |
| Performance regression | Low | Low | String generation faster than maps |

---

## Questions for Review

Before proceeding, please confirm:

1. ✅ Is it acceptable to remove ClientConfig entirely? (or should we keep it as a fallback?)
2. ✅ Is string-based YAML generation acceptable? (vs PropertySource approach)
3. ✅ Should we add a default config file for local development without TestContainers?
4. ✅ Any concerns about removing the hardcoded fallback client?
5. ✅ Approval to push to GitHub and run full CI/CD verification?

---

## Implementation Checklist

### Before Starting
- [ ] Review this plan with stakeholders
- [ ] Confirm all questions above
- [ ] Create feature branch `feature/simplify-config`

### During Implementation (Phase 1-3)
- [ ] Write unit tests for YAML generation
- [ ] Delete ClientConfig.java
- [ ] Implement StringBuilder-based YAML generation
- [ ] All local tests pass
- [ ] Code review complete

### Before GitHub Push (Phase 4)
- [ ] oauth2-server builds locally
- [ ] dynamic-form tests pass locally with new oauth2-server
- [ ] No compilation errors
- [ ] No test failures

### GitHub Actions (Phase 4)
- [ ] oauth2-server build succeeds
- [ ] Docker image published
- [ ] dynamic-form tests triggered and pass
- [ ] All workflow checks pass

### After Merge (Phase 5)
- [ ] Update CLAUDE.md
- [ ] Update README if needed
- [ ] Document sample config files
- [ ] Create PR with full explanation
- [ ] Code review and approval
- [ ] Merge to master

---

## Next Steps

1. Review this plan
2. Answer the 5 review questions above
3. Proceed with Phase 1 when approved
4. Create weekly check-in points to verify progress

---

**Document Created:** 2026-06-13  
**Status:** Ready for Review  
**Last Updated:** 2026-06-13

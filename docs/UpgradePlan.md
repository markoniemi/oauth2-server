# Spring Boot 4.0.3 & Spring Security 7.0 Upgrade Plan

**Target Release:** Spring Boot 4.0.3, Spring Security 7.0  
**Current Release:** Spring Boot 3.5.6  
**Date:** 2026-09-27  
**Status:** Planning

## Executive Summary

Upgrade OAuth2 Authorization Server from Spring Boot 3.5.6 to 4.0.3, including Spring Security 7.0 migration. Primary driver: align with dynamic-form project dependency track and leverage latest OAuth2 configuration model.

**Key Risk:** Spring Security 7.0 significantly changes OAuth2 AuthorizationServer configuration API. Must verify compatibility with dynamic-form before merge.

---

## Phase 1: Planning & Dependency Analysis (2-3 days)

### Goals
- Verify dynamic-form compatibility requirements
- Audit TestContainers 2.0.3 support in Boot 4.0.3
- Identify all configuration breaking changes
- Lock timeline with dynamic-form team

### Tasks
1. **Dependency Analysis**
   - Check Spring Security 7.0 OAuth2 authorization server breaking changes
   - Verify TestContainers 2.0.3 works with Boot 4.0.3 (no known issues)
   - Document all deprecated Spring Boot 3.5.6 properties in `application.yml`

2. **Downstream Impact**
   - Review dynamic-form `OAuth2TestContainersUsage.md` for API dependencies
   - Check JWT token claim structure compatibility
   - Verify OIDC discovery endpoint response format unchanged
   - Confirm client authentication flows support in new API

3. **Acceptance Criteria**
   - [ ] All Spring Boot 3→4 breaking changes identified
   - [ ] Spring Security 7.0 OAuth2 API differences documented
   - [ ] TestContainers compatibility confirmed (run container test)
   - [ ] dynamic-form team agrees on test timeline

---

## Phase 2: Dependency Upgrade (1-2 days)

### Goals
- Update pom.xml with new versions
- Identify deprecated property warnings
- Stage for configuration migration

### Tasks
1. **Update pom.xml**
   ```xml
   <!-- Update parent -->
   <parent>
     <version>4.0.3</version>
   </parent>
   
   <!-- Add properties-migrator (temporary) -->
   <dependency>
     <groupId>org.springframework.boot</groupId>
     <artifactId>spring-boot-properties-migrator</artifactId>
     <scope>runtime</scope>
   </dependency>
   ```

2. **Test Compilation & Properties Scan**
   - Run `mvn clean compile` to trigger properties-migrator
   - Capture WARN logs for deprecated properties
   - Update `application.yml` with new property names
   - Remove properties-migrator after migration complete

3. **Verify TestContainers Compatibility**
   - Run container integration tests (should pass)
   - Confirm JIB Docker image builds successfully
   - Check container startup logs for warnings

4. **Acceptance Criteria**
   - [ ] pom.xml updated to Boot 4.0.3, Security 7.0
   - [ ] Project compiles without errors
   - [ ] All deprecated properties logged and addressed
   - [ ] Container integration test passes
   - [ ] properties-migrator removed

---

## Phase 3: Spring Security 7.0 OAuth2 Configuration Migration (2-3 days)

### Goals
- Migrate SecurityFilterChain to new OAuth2AuthorizationServer API
- Preserve token generation and OIDC discovery behavior
- Update client/user configuration approach

### Key Breaking Changes

**Old API (Spring Security 6.x):**
```java
http.authorizeRequests()
  .requestMatchers(OidcProviderConfigurationEndpointFilter.DEFAULT_OIDC_PROVIDER_CONFIGURATION_ENDPOINT_URI)
  .authenticated()
  .and()
  .csrf().ignoringRequestMatchers(...)
```

**New API (Spring Security 7.0):**
```java
http.oauth2AuthorizationServer((authorizationServer) ->
  authorizationServer
    .registeredClientRepository(registeredClientRepository)
    .authorizationServerSettings(authorizationServerSettings)
    .tokenGenerator(tokenGenerator)
    .clientAuthentication(...)
    .authorizationEndpoint(...)
    .tokenEndpoint(...)
    .oidc(oidc -> oidc
      .providerConfigurationEndpoint(...)
      .userInfoEndpoint(...)
    )
);
```

### Migration Tasks

1. **Update SecurityFilterChain Configuration**
   - Replace old `http.authorizeRequests()` with `http.oauth2AuthorizationServer()`
   - Move OAuth2 setup into new configurer pattern
   - Import `OAuth2AuthorizationServerConfiguration` for defaults
   - Update CORS/CSRF settings within new model

2. **Verify Token Generation**
   - Confirm JWT token structure unchanged (iss, sub, aud claims)
   - Check token expiry times match configuration
   - Verify OIDC scope claims included (`scope`, `auth_time`)

3. **Test OIDC Discovery Endpoint**
   - Call `/.well-known/openid-configuration`
   - Verify response includes all required endpoints
   - Confirm metadata claims match Boot 3.5.6 output

4. **Update TestContainers Integration**
   - TestContainers API unchanged; verify container config passes
   - Check that custom client/user YAML configs still load
   - Run OAuth2TestContainersUsage tests fully

5. **Acceptance Criteria**
   - [ ] SecurityFilterChain uses new OAuth2AuthorizationServer API
   - [ ] Application starts without errors
   - [ ] Token generation produces same JWT structure
   - [ ] OIDC endpoints respond identically to Boot 3.5.6
   - [ ] All unit tests pass
   - [ ] TestContainers integration tests pass

---

## Phase 4: Local Testing & Validation (1-2 days)

### Goals
- Verify all authorization flows work end-to-end
- Confirm API backward compatibility
- Build confidence before downstream testing

### Tasks
1. **Authorization Code Flow Test**
   - Start server locally
   - Call `/oauth2/authorize` endpoint
   - Complete login flow
   - Verify token endpoint returns valid JWT

2. **Token Introspection & Revocation**
   - Test `/oauth2/introspect` endpoint
   - Verify `/oauth2/revoke` endpoint
   - Check user info endpoint (`/userinfo`)

3. **OIDC Discovery & Metadata**
   - Verify `/.well-known/openid-configuration` response
   - Compare with Boot 3.5.6 baseline (if available)
   - Confirm all required endpoints listed

4. **Client Authentication**
   - Test client secret authentication (POST /oauth2/token)
   - Test PKCE public client flow
   - Verify client assertion (JWT bearer) authentication

5. **Acceptance Criteria**
   - [ ] All authorization flows work end-to-end
   - [ ] Token structure matches dynamic-form expectations
   - [ ] OIDC metadata correct and complete
   - [ ] Container tests pass with Boot 4.0.3

---

## Phase 5: Dynamic-Form Integration Testing (2-3 days)

### Goals
- Verify compatibility with downstream application
- Catch any JWT claim or OIDC endpoint surprises
- Coordinate timing with dynamic-form team

### Tasks
1. **Run dynamic-form Test Suite**
   - Point dynamic-form to upgraded OAuth2 server
   - Run all OAuth2 integration tests
   - Verify login flow works end-to-end
   - Check role/permission claims in JWT

2. **Smoke Test dynamic-form Features**
   - Login as different user roles
   - Create/edit forms with different permissions
   - Verify token refresh (if supported)
   - Check token expiry behavior

3. **Document Any Behavioral Changes**
   - List any JWT claim differences
   - Note endpoint response format changes (if any)
   - Capture timing differences (token generation speed)

4. **Acceptance Criteria**
   - [ ] All dynamic-form OAuth2 tests pass
   - [ ] Login flow works with upgraded server
   - [ ] JWT claims match dynamic-form expectations
   - [ ] No regressions in user/role management
   - [ ] dynamic-form team confirms compatibility

---

## Phase 6: Documentation & Release (1 day)

### Goals
- Document changes for maintainers
- Prepare for merge to main
- Tag release version

### Tasks
1. **Update Project Documentation**
   - Update [TechSpec.md](TechSpec.md) with new Spring Security 7.0 config model
   - Update [OAuth2TestContainersUsage.md](../OAuth2TestContainersUsage.md) if API changed
   - Document any new configuration options in README

2. **Create Release Notes**
   - List all breaking changes for downstream projects
   - Document JWT claim structure (if changed)
   - Include migration steps for TestContainers users

3. **Code Review Prep**
   - Create PR with clear description of changes
   - Link to Spring Security 7.0 migration guide
   - Tag dynamic-form maintainers for review

4. **Acceptance Criteria**
   - [ ] All docs updated
   - [ ] PR created and ready for review
   - [ ] Release notes complete
   - [ ] dynamic-form maintainers notified

---

## Risk Mitigation

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Spring Security 7.0 API changes break OAuth2 flow | High | Phase 3 thorough testing; Phase 5 dynamic-form validation |
| TestContainers incompatibility with Boot 4 | Medium | Phase 2 container test; verify JIB image builds |
| JWT claim structure changes break dynamic-form | High | Phase 4 token validation; Phase 5 integration test |
| OIDC discovery endpoint format changes | Medium | Phase 4 metadata verification; compare with baseline |
| New Spring Boot 4 deprecations | Low | Phase 2 properties-migrator handles; update props |

---

## Dependencies

- **dynamic-form project**: Must complete Phase 5 testing before merge to main
- **Spring Security 7.0 documentation**: Referenced for API migration
- **TestContainers 2.0.3**: Verify compatibility in Phase 2

---

## Timeline Estimate

- **Phase 1**: 2-3 days (analysis + dynamic-form coordination)
- **Phase 2**: 1-2 days (dependency updates + container test)
- **Phase 3**: 2-3 days (configuration migration + unit tests)
- **Phase 4**: 1-2 days (local end-to-end testing)
- **Phase 5**: 2-3 days (dynamic-form integration + fixes if needed)
- **Phase 6**: 1 day (docs + release)

**Total**: 9-14 days (accounting for testing cycles)

---

## Rollback Plan

If Phase 5 integration testing fails:
1. Revert all changes to Spring Boot 3.5.6
2. Document blocking issue in GitHub issue
3. Wait for Spring Boot 4.1 or later if needed for stability
4. Consider phased migration with feature branch experiment

---

## Sign-Off

- [ ] OAuth2 server maintainer agrees to plan
- [ ] dynamic-form maintainer agrees to Phase 5 timeline
- [ ] Team confirms resource availability

# Code Review Findings

## 1. Must Fix

### 1.1 SecurityConfig:90 — NoOpPasswordEncoder in production code
**Issue:** Using `NoOpPasswordEncoder` bypasses password encryption even in main codebase.

**Fix:** Replace with `BCryptPasswordEncoder`:
```java
@Bean
public PasswordEncoder passwordEncoder() {
  return new BCryptPasswordEncoder();
}
```

### 1.2 ClientConfig:23-26 — Static field mutation causes concurrency issues
**Issue:** `setClients()` mutates shared static state with no thread safety. Lifecycle unclear when tests run in parallel or multiple containers spin up.

**Fix:** Refactor to dependency injection. Store clients in `ServerProperties` or environment-driven config instead of static setter:
```java
@Configuration
public class ClientConfig {
  private final ServerProperties serverProps;
  
  public ClientConfig(ServerProperties serverProps) {
    this.serverProps = serverProps;
  }
  
  @Bean
  public RegisteredClientRepository testcontainersRegisteredClientRepository() {
    List<Client> clients = serverProps.getClients();
    // ... use clients directly
  }
}
```

### 1.3 OAuth2Container:88-154 — Cryptic nested map construction for YAML generation
**Issue:** 67-line method with deeply nested LinkedHashMap construction is hard to follow. Mixes business logic (user/client mapping) with YAML structure building.

**Fix:** Extract to `YamlConfigBuilder` class:
```java
private void generateAndMountConfigYaml() throws IOException {
  if (users.isEmpty() && clients.isEmpty()) return;
  
  YamlConfigBuilder builder = new YamlConfigBuilder();
  if (!users.isEmpty()) builder.addUsers(users);
  if (!clients.isEmpty()) builder.addClients(clients);
  
  File configFile = builder.build(Files.createTempDirectory("oauth2-config-"));
  withCopyFileToContainer(MountableFile.forHostPath(configFile.getAbsolutePath()), "/config/application.yaml");
  // ... cleanup
}
```

Also: `configure()` runs on every instance startup. Move template YAML setup to static initialization or separate builder method to avoid redundant work.

---

## 2. Should Fix

### 2.1 Client.java:27-28, 31-32, 36-37 — Inconsistent spacing around `=`
**Issue:** Setter methods lack spaces: `this.redirectUris=new HashSet<>(...)` violates code style.

**Fix:** Add spaces around `=`:
```java
public Client withRedirectUris(String... uris) {
  this.redirectUris = new HashSet<>(Arrays.asList(uris));
  return this;
}
```

### 2.2 Client.java, User.java — `@Data` makes fields mutable
**Issue:** `@Data` + `@RequiredArgsConstructor` generates getters and setters. Fields should be immutable for config objects.

**Fix:** Use `@Value` or Java records (Java 21 available):
```java
@Value
@RequiredArgsConstructor
public class Client {
  @NotBlank(message = "Client ID cannot be blank")
  String clientId;
  
  @NotBlank(message = "Client secret cannot be blank")
  String clientSecret;
  
  Set<String> redirectUris = new HashSet<>();
  // ... rest
}
```

Or better, use Java records:
```java
public record Client(
  @NotBlank(message = "Client ID cannot be blank") String clientId,
  @NotBlank(message = "Client secret cannot be blank") String clientSecret,
  Set<String> redirectUris,
  Set<String> scopes,
  Set<String> grantTypes
) {}
```

### 2.3 SecurityConfig:101-115 — URI parsing repeated per redirect URI
**Issue:** `new java.net.URI(uri)` called twice per URI in loop. Fragile try-catch swallows parse errors silently.

**Fix:** Extract helper method:
```java
private String extractOrigin(String uri) {
  try {
    java.net.URI parsed = new java.net.URI(uri);
    return parsed.getScheme() + "://" + parsed.getAuthority();
  } catch (java.net.URISyntaxException e) {
    return uri; // Fallback
  }
}

// In corsConfigurationSource():
if (clients != null) {
  clients.forEach(client ->
    client.getRedirectUris().forEach(uri -> origins.add(extractOrigin(uri)))
  );
}
```

### 2.4 ServerConfig:16-21 — Explicit constructor conflicts with `@Builder`
**Issue:** Manual constructor duplicates Lombok-generated one. If fields change, maintenance burden increases.

**Fix:** Remove explicit constructor, rely on `@Builder`:
```java
@Data
@Builder
public class ServerConfig {
  @Builder.Default
  private final List<User> users = new ArrayList<>();
  
  @Builder.Default
  private final List<Client> clients = new ArrayList<>();
  
  private String issuerUrl;
  private String contextPath;
}
```

### 2.5 OAuth2Container:60-61 — `getAuthServerUrl()` hardcodes localhost
**Issue:** Returns `http://localhost:` + port. Fails when container runs on remote host or docker-compose network.

**Fix:** Add optional hostname parameter:
```java
private String hostname = "localhost";

public OAuth2Container withHostname(String hostname) {
  this.hostname = hostname;
  return this;
}

public String getAuthServerUrl() {
  return "http://" + hostname + ":" + getMappedPort(AUTH_SERVER_PORT);
}
```

---

## 3. Consider

### 3.1 ServerConfig — Use Java records instead of Lombok
Java 21 is target. Records are immutable by design and more concise:
```java
public record ServerConfig(
  List<User> users,
  List<Client> clients,
  String issuerUrl,
  String contextPath
) {
  public ServerConfig(List<User> users, List<Client> clients, String issuerUrl, String contextPath) {
    this.users = users == null ? List.of() : List.copyOf(users);
    this.clients = clients == null ? List.of() : List.copyOf(clients);
    this.issuerUrl = issuerUrl;
    this.contextPath = contextPath;
  }
}
```

### 3.2 ClientConfig:66-92 — String matching for grant types is fragile
Using string literals ("authorization_code", "refresh_token") for grant type mapping. If enum values change upstream, mapping breaks silently.

**Consider:** Use enum-based conversion:
```java
enum GrantTypeMapper {
  AUTHORIZATION_CODE("authorization_code", AuthorizationGrantType.AUTHORIZATION_CODE),
  REFRESH_TOKEN("refresh_token", AuthorizationGrantType.REFRESH_TOKEN),
  CLIENT_CREDENTIALS("client_credentials", AuthorizationGrantType.CLIENT_CREDENTIALS);
  
  private final String literal;
  private final AuthorizationGrantType type;
  
  GrantTypeMapper(String literal, AuthorizationGrantType type) {
    this.literal = literal;
    this.type = type;
  }
  
  static AuthorizationGrantType toType(String literal) {
    return Arrays.stream(values())
      .filter(g -> g.literal.equals(literal))
      .map(g -> g.type)
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Unknown grant type: " + literal));
  }
}
```

### 3.3 SecurityProperties — Add field validation
List fields accept empty config without warning. Catch early:
```java
@Data
@Configuration
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
  @NotEmpty(message = "Users list cannot be empty")
  private List<User> users = new ArrayList<>();
  // ...
}
```

---

## 4. Dependencies

### 4.1 Spring Boot 3.5.6
**Status:** Patch version 3.5.14 available. Major version 4.0.3+ also available.

**Patch upgrade (3.5.6 → 3.5.14):**
- Safe. Security patches and bug fixes only. No breaking changes.
- Recommended.

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.5.14</version>  <!-- was 3.5.6 -->
  <relativePath/>
</parent>
```

**Major upgrade (3.5.6 → 4.0.3+):**
- Java 21+ compatible (project target already Java 21).
- Breaking changes: Requires review of OAuth2 authorization server API changes, deprecated removals, naming changes.
- Security config API may differ; test thoroughly.
- **Defer unless migration effort justified.** Option for future roadmap.

Consider Spring Boot 4 only if project roadmap explicitly requires it or if 3.5 reaches EOL.

### 4.2 TestContainers 2.0.3
**Status:** Current. No newer version available in Java 21 range.

No action needed.

### 4.3 HtmlUnit 2.70.0
**Status:** Stable. Version 2.70.0 is recent and maintained. No critical updates pending.

No action needed.

### 4.4 Lombok 1.18.40
**Status:** Current. Compiler plugin version matches dependency version (1.18.40).

No action needed.

### 4.5 Jakarta Validation & Hibernate Validator
**Status:** Managed by Spring Boot BOM. Versions align with Spring Boot 3.5.6.

On upgrade to Spring Boot 3.5.14, these automatically update to compatible versions.

### 4.6 Maven Compiler Plugin 3.11.0 vs 3.13.0
**Note:** pom.xml specifies plugin version 3.13.0 in `<configuration>` (line 99) but parent pom may use different version. Verify consistency:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <version>3.13.0</version>  <!-- explicit version OK, but check parent default -->
  ...
</plugin>
```

Consider letting Spring Boot BOM manage version unless you need a specific feature.

---

## 5. Summary

**Priority order:** Fix NoOpPasswordEncoder (security) → refactor ClientConfig static state (correctness/concurrency) → extract YAML builder (maintainability). Secondary: spacing, immutability, URI parsing. Consider: records, enum mapping, validation.

**Dependency upgrades:** Spring Boot 3.5.6 → 3.5.14 (patch); all other dependencies current.

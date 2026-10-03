package com.example.auth.testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class OAuth2ContainerConfigTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    @Test
    public void noUsersOrClientsGeneratesNoConfig() throws Exception {
        assertNull(new OAuth2Container().generateConfigYaml());
    }

    @Test
    public void usersAreWrittenUnderAppSecurity() throws Exception {
        OAuth2Container container = new OAuth2Container().withUser("admin", "secret", "ADMIN");

        Map<String, Object> config = parse(container.generateConfigYaml());

        Map<String, Object> user = first(at(config, "app", "security", "users"));
        assertEquals("admin", user.get("username"));
        assertEquals("secret", user.get("password"));
        assertEquals(List.of("ADMIN"), user.get("roles"));
    }

    @Test
    public void confidentialClientIsWrittenAsBootRegistration() throws Exception {
        OAuth2Container container = new OAuth2Container()
            .withOAuth2Client(new Client("test-client", "test-secret")
                .withRedirectUris("http://localhost:3000/callback")
                .withScopes("openid")
                .withGrantTypes("authorization_code"));

        Map<String, Object> registration = at(parse(container.generateConfigYaml()),
            "spring", "security", "oauth2", "authorizationserver", "client", "test-client", "registration");

        assertEquals("test-client", registration.get("client-id"));
        assertEquals("test-secret", registration.get("client-secret"));
        assertEquals(List.of("client_secret_basic"), registration.get("client-authentication-methods"));
        assertEquals(List.of("authorization_code"), registration.get("authorization-grant-types"));
        assertEquals(List.of("http://localhost:3000/callback"), registration.get("redirect-uris"));
        assertEquals(List.of("openid"), registration.get("scopes"));
    }

    @Test
    public void confidentialClientRequiringProofKeyWritesClientLevelSetting() throws Exception {
        OAuth2Container container = new OAuth2Container()
            .withOAuth2Client(new Client("test-client", "test-secret").withRequireProofKey(true));

        Map<String, Object> client = clientEntry(container, "test-client");

        assertEquals(true, client.get("require-proof-key"));
        assertFalse(((Map<?, ?>) client.get("registration")).containsKey("client-settings"));
    }

    @Test
    public void confidentialClientWithoutProofKeyDisablesItExplicitly() throws Exception {
        // Spring Authorization Server requires PKCE by default, so false must be written explicitly
        OAuth2Container container = new OAuth2Container()
            .withOAuth2Client(new Client("test-client", "test-secret"));

        assertEquals(false, clientEntry(container, "test-client").get("require-proof-key"));
    }

    @Test
    public void publicClientUsesNoneAuthenticationAndRequiresProofKey() throws Exception {
        OAuth2Container container = new OAuth2Container()
            .withOAuth2Client(new Client("frontend-client", "")
                .withRedirectUris("http://localhost:5173"));

        Map<String, Object> client = clientEntry(container, "frontend-client");
        Map<String, Object> registration = at(client, "registration");

        assertEquals(true, client.get("require-proof-key"));
        assertEquals(List.of("none"), registration.get("client-authentication-methods"));
        assertFalse(registration.containsKey("client-secret"));
        assertEquals(List.of("authorization_code"), registration.get("authorization-grant-types"));
    }

    @Test
    public void postLogoutRedirectUrisAreWritten() throws Exception {
        OAuth2Container container = new OAuth2Container()
            .withOAuth2Client(new Client("frontend-client", "")
                .withPostLogoutRedirectUris("http://localhost:5173"));

        Map<String, Object> registration = at(clientEntry(container, "frontend-client"), "registration");

        assertEquals(List.of("http://localhost:5173"), registration.get("post-logout-redirect-uris"));
    }

    @Test
    public void tokenTimeToLivesAreWrittenOnlyWhenSet() throws Exception {
        OAuth2Container container = new OAuth2Container()
            .withOAuth2Client(new Client("with-ttl", "secret")
                .withAccessTokenTimeToLive(Duration.ofHours(1))
                .withRefreshTokenTimeToLive(Duration.ofDays(7)))
            .withOAuth2Client(new Client("without-ttl", "secret"));

        Map<String, Object> token = at(clientEntry(container, "with-ttl"), "token");

        assertEquals("PT1H", token.get("access-token-time-to-live"));
        assertEquals("PT168H", token.get("refresh-token-time-to-live"));
        assertFalse(clientEntry(container, "without-ttl").containsKey("token"));
    }

    @Test
    public void corsOriginsAreDerivedFromClientRedirectUris() throws Exception {
        OAuth2Container container = new OAuth2Container()
            .withOAuth2Client(new Client("frontend-client", "")
                .withRedirectUris("http://localhost:5173/callback", "http://localhost:8080")
                .withPostLogoutRedirectUris("http://app.local:3000/"));

        List<String> origins = at(parse(container.generateConfigYaml()), "app", "cors", "allowed-origins");

        assertEquals(Set.of("http://localhost:5173", "http://localhost:8080", "http://app.local:3000"),
            Set.copyOf(origins));
        assertEquals(3, origins.size());
    }

    @Test
    public void usersOnlyConfigLeavesCorsDefaults() throws Exception {
        OAuth2Container container = new OAuth2Container().withUser("admin", "secret", "ADMIN");

        assertNull(at(parse(container.generateConfigYaml()), "app", "cors"));
    }

    @Test
    public void issuerUrlIsPassedToServer() {
        OAuth2Container container = new OAuth2Container().withIssuerUrl("http://auth.example:9000");

        assertEquals("http://auth.example:9000",
            container.getEnvMap().get("SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER"));
        assertEquals("http://auth.example:9000", container.getIssuerUrl());
    }

    @Test
    public void contextPathIsPassedToServer() {
        OAuth2Container container = new OAuth2Container().withContextPath("/auth");

        assertEquals("/auth", container.getEnvMap().get("SERVER_SERVLET_CONTEXT_PATH"));
    }

    private static Map<String, Object> clientEntry(OAuth2Container container, String clientId)
        throws Exception {
        return at(parse(container.generateConfigYaml()),
            "spring", "security", "oauth2", "authorizationserver", "client", clientId);
    }

    private static Map<String, Object> parse(String yaml) throws Exception {
        return YAML.readValue(yaml, Map.class);
    }

    @SuppressWarnings("unchecked")
    static <T> T at(Map<String, Object> map, String... path) {
        Object current = map;
        for (String key : path) {
            current = ((Map<String, Object>) current).get(key);
        }
        return (T) current;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> first(Object list) {
        return ((List<Map<String, Object>>) list).get(0);
    }
}

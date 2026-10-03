package com.example.auth.testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.util.List;
import java.util.Map;
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

package com.example.auth.testcontainers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tests for the simplified YAML generation in OAuth2Container.
 * Validates that the new string-based YAML generation produces valid,
 * parseable YAML that matches the expected Spring Boot OAuth2 configuration structure.
 */
public class OAuth2ContainerYamlGenerationTest {

    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    /**
     * Test that generated YAML is valid and parseable
     */
    @Test
    public void generatedYamlIsValid() throws IOException {
        OAuth2Container container = new OAuth2Container()
            .withUser("admin", "admin", "USER", "ADMIN")
            .withOAuth2Client(new Client("test-client", "")
                .withRedirectUris("http://localhost:8080")
                .withScopes("openid", "profile"));

        String generatedYaml = container.generateConfigYaml();

        assertNotNull(generatedYaml);
        assertFalse(generatedYaml.isEmpty());

        // Should be parseable as YAML
        assertDoesNotThrow(() -> yamlMapper.readValue(generatedYaml, Map.class));
    }

    /**
     * Test that users section is correctly generated
     */
    @Test
    public void usersAreCorrectlyGenerated() throws IOException {
        OAuth2Container container = new OAuth2Container()
            .withUser("admin", "admin-password", "ADMIN", "USER")
            .withUser("viewer", "viewer-password", "VIEWER");

        String generatedYaml = container.generateConfigYaml();
        Map<String, Object> config = yamlMapper.readValue(generatedYaml, Map.class);

        // Verify structure: app.security.users
        Map<String, Object> app = (Map<String, Object>) config.get("app");
        assertNotNull(app);

        Map<String, Object> security = (Map<String, Object>) app.get("security");
        assertNotNull(security);

        List<Map<String, Object>> users = (List<Map<String, Object>>) security.get("users");
        assertNotNull(users);
        assertEquals(2, users.size());

        // Verify first user
        Map<String, Object> adminUser = users.get(0);
        assertEquals("admin", adminUser.get("username"));
        assertEquals("admin-password", adminUser.get("password"));
        assertNotNull(adminUser.get("roles"));
    }

    /**
     * Test that clients section is correctly generated
     */
    @Test
    public void clientsAreCorrectlyGenerated() throws IOException {
        OAuth2Container container = new OAuth2Container()
            .withOAuth2Client(new Client("app1", "secret1")
                .withRedirectUris("http://localhost:8080/callback", "http://localhost:3000")
                .withScopes("openid", "profile", "email"));

        String generatedYaml = container.generateConfigYaml();
        Map<String, Object> config = yamlMapper.readValue(generatedYaml, Map.class);

        // Verify structure: spring.security.oauth2.authorizationserver.client
        Map<String, Object> spring = (Map<String, Object>) config.get("spring");
        assertNotNull(spring);

        Map<String, Object> oauth2 = (Map<String, Object>)
            ((Map<String, Object>) spring.get("security")).get("oauth2");
        assertNotNull(oauth2);

        Map<String, Object> authserver = (Map<String, Object>)
            oauth2.get("authorizationserver");
        assertNotNull(authserver);

        Map<String, Object> clients = (Map<String, Object>) authserver.get("client");
        assertNotNull(clients);
        assertTrue(clients.containsKey("app1"));
    }

    /**
     * Test that empty users/clients don't break YAML generation
     */
    @Test
    public void emptyUsersAndClientsProducesValidYaml() throws IOException {
        OAuth2Container container = new OAuth2Container();

        String generatedYaml = container.generateConfigYaml();

        // Should still be valid YAML even if empty
        assertDoesNotThrow(() -> yamlMapper.readValue(generatedYaml, Map.class));
    }

    /**
     * Test that YAML indentation is correct (critical for YAML parsing)
     */
    @Test
    public void yamlIndentationIsCorrect() {
        OAuth2Container container = new OAuth2Container()
            .withUser("admin", "password", "ADMIN");

        String generatedYaml = container.generateConfigYaml();

        // Should have proper indentation markers
        assertTrue(generatedYaml.contains("app:"));
        assertTrue(generatedYaml.contains("  security:"));
        assertTrue(generatedYaml.contains("    users:"));

        // Verify it doesn't have broken indentation (double spaces at line start)
        String[] lines = generatedYaml.split("\n");
        for (String line : lines) {
            if (line.isEmpty()) continue;
            // Line should either start with no spaces or have consistent indentation
            assertTrue(
                !line.startsWith("   ") || line.startsWith("    "),
                "Line has inconsistent indentation: " + line
            );
        }
    }

    /**
     * Test that multiple users and clients are all included
     */
    @Test
    public void multipleUsersAndClientsAreIncluded() throws IOException {
        OAuth2Container container = new OAuth2Container()
            .withUser("admin", "pass1", "ADMIN")
            .withUser("user", "pass2", "USER")
            .withUser("viewer", "pass3", "VIEWER")
            .withOAuth2Client(new Client("app1", "secret1")
                .withRedirectUris("http://localhost:8080"))
            .withOAuth2Client(new Client("app2", "secret2")
                .withRedirectUris("http://localhost:3000"));

        String generatedYaml = container.generateConfigYaml();
        Map<String, Object> config = yamlMapper.readValue(generatedYaml, Map.class);

        // Count users
        Map<String, Object> app = (Map<String, Object>) config.get("app");
        Map<String, Object> security = (Map<String, Object>) app.get("security");
        List<Map<String, Object>> users = (List<Map<String, Object>>) security.get("users");
        assertEquals(3, users.size(), "All 3 users should be in YAML");

        // Count clients
        Map<String, Object> spring = (Map<String, Object>) config.get("spring");
        Map<String, Object> oauth2 = (Map<String, Object>)
            ((Map<String, Object>) spring.get("security")).get("oauth2");
        Map<String, Object> authserver = (Map<String, Object>) oauth2.get("authorizationserver");
        Map<String, Object> clients = (Map<String, Object>) authserver.get("client");
        assertEquals(2, clients.size(), "Both clients should be in YAML");
    }
}

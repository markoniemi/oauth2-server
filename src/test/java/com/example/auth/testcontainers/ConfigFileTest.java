package com.example.auth.testcontainers;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigFileTest {

    @Test
    public void loadUsersAndClientsFromConfigFile() throws IOException {
        Container container = new Container()
            .withConfigFile("test-config.yaml");

        // Verify users loaded from config
        assertFalse(container.getUsers().isEmpty());
        assertEquals(2, container.getUsers().size());

        User admin = container.getUsers().stream()
            .filter(u -> "config-admin".equals(u.getUsername()))
            .findFirst()
            .orElse(null);

        assertNotNull(admin);
        assertEquals("password123", admin.getPassword());
        assertEquals(Set.of("ADMIN", "USER"), admin.getRoles());

        User user = container.getUsers().stream()
            .filter(u -> "config-user".equals(u.getUsername()))
            .findFirst()
            .orElse(null);

        assertNotNull(user);
        assertEquals("password456", user.getPassword());
        assertEquals(Set.of("USER"), user.getRoles());

        // Verify clients loaded from config
        assertFalse(container.getClients().isEmpty());
        assertEquals(1, container.getClients().size());

        Client client = container.getClients().get(0);
        assertEquals("config-client", client.getClientId());
        assertTrue(client.getRedirectUris().contains("http://localhost:3000/callback"));
        assertTrue(client.getScopes().containsAll(Set.of("openid", "profile")));
        assertTrue(client.getGrantTypes().contains("authorization_code"));
    }

    @Test
    public void configFileNotFound() {
        Container container = new Container();
        assertThrows(IOException.class, () -> {
            container.withConfigFile("nonexistent.yaml");
        });
    }

    @Test
    public void configFileWithOnlyUsers() throws IOException {
        Container container = new Container()
            .withConfigFile("test-config.yaml");

        // Ensure users are loaded even if we only check that part
        assertFalse(container.getUsers().isEmpty());
        assertTrue(container.getUsers().stream()
            .anyMatch(u -> "config-admin".equals(u.getUsername())));
    }

    @Test
    public void configFileWithOnlyClients() throws IOException {
        Container container = new Container()
            .withConfigFile("test-config.yaml");

        // Ensure clients are loaded even if we only check that part
        assertFalse(container.getClients().isEmpty());
        assertTrue(container.getClients().stream()
            .anyMatch(c -> "config-client".equals(c.getClientId())));
    }

    @Test
    public void combineConfigFileWithFluentApi() throws IOException {
        Container container = new Container()
            .withConfigFile("test-config.yaml")
            .withUser("extra-user", "password", "EXTRA_ROLE")
            .withOAuth2Client(new Client("api-client", "secret")
                .withScopes("api")
                .withRedirectUris("http://api.example.com/callback"));

        // Users from config plus fluent API
        assertEquals(3, container.getUsers().size());
        assertTrue(container.getUsers().stream()
            .anyMatch(u -> "extra-user".equals(u.getUsername())));

        // Clients from config plus fluent API
        assertEquals(2, container.getClients().size());
        assertTrue(container.getClients().stream()
            .anyMatch(c -> "api-client".equals(c.getClientId())));
    }
}

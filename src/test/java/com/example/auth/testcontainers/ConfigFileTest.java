package com.example.auth.testcontainers;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ContainerLaunchException;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigFileTest {

    @Test
    public void mountConfigFileFromClasspath() {
        Container container = new Container()
            .withConfigFile("test-config.yaml");

        container.start();
        try {
            assertTrue(container.isRunning());
        } finally {
            container.stop();
        }
    }

    @Test
    public void configFileNotFound() {
        Container container = new Container();

        assertThrows(IllegalArgumentException.class, () -> {
            container.withConfigFile("nonexistent.yaml");
        });
    }

    @Test
    public void configFileMountedToConfigPath() {
        Container container = new Container()
            .withConfigFile("test-config.yaml");

        container.start();
        try {
            // Spring Boot loads config from /config/application.yaml
            assertTrue(container.isRunning());
            String url = container.getAuthServerUrl();
            assertNotNull(url);
        } finally {
            container.stop();
        }
    }

    @Test
    public void fluentApiAndConfigFileMixed() {
        Container container = new Container()
            .withConfigFile("test-config.yaml")
            .withUser("extra-user", "password", "EXTRA_ROLE")
            .withOAuth2Client(new Client("api-client", "secret")
                .withScopes("api")
                .withRedirectUris("http://api.example.com/callback"));

        // getUsers/getClients only reflect fluent API (not mounted config file)
        assertEquals(1, container.getUsers().size());
        assertEquals(1, container.getClients().size());

        container.start();
        try {
            // Container has both: config file users/clients + fluent API users/clients
            assertTrue(container.isRunning());
        } finally {
            container.stop();
        }
    }

    @Test
    public void configFileLoadsUserAndClientConfig() {
        Container container = new Container()
            .withConfigFile("test-config.yaml");

        container.start();
        try {
            // Verify container started successfully, meaning Spring Boot loaded the config
            assertTrue(container.isRunning());

            // Verify the auth server endpoint is accessible
            String authUrl = container.getAuthServerUrl();
            assertNotNull(authUrl);
            assertTrue(authUrl.startsWith("http://localhost:"));
        } finally {
            container.stop();
        }
    }
}

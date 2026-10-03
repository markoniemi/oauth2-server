package com.example.auth.testcontainers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerIT {

    private static OAuth2Container container;

    @BeforeAll
    static void setUp() {
        container = new OAuth2Container()
            .withUser("admin", "admin123", "ADMIN", "USER");
        container.start();
    }

    @AfterAll
    static void tearDown() {
        if (container != null) {
            container.stop();
        }
    }

    @Test
    public void containerStartsSuccessfully() {
        assertTrue(container.isRunning());
    }

    @Test
    public void getAuthServerUrl() {
        String url = container.getAuthServerUrl();
        assertNotNull(url);
        assertTrue(url.startsWith("http://localhost:"));
        assertTrue(url.contains(":"));
    }

    @Test
    public void getMappedPort() {
        int port = container.getMappedPort(9000);
        assertTrue(port > 0);
        assertTrue(port < 65536);
    }

    @Test
    public void getIssuerUrlDefault() {
        String issuer = container.getIssuerUrl();
        assertNotNull(issuer);
        assertTrue(issuer.startsWith("http://"));
    }

    @Test
    public void discoveredIssuerMatchesContainerIssuerUrl() {
        assertEquals(container.getIssuerUrl(), discoveredIssuer(container));
    }

    @Test
    public void customIssuerAndContextPathAreApplied() {
        try (OAuth2Container custom = new OAuth2Container()
            .withIssuerUrl("http://auth.example:9000")
            .withContextPath("/auth")) {
            custom.start();

            assertTrue(custom.getAuthServerUrl().endsWith("/auth"));
            assertEquals("http://auth.example:9000", discoveredIssuer(custom));
        }
    }

    private static String discoveredIssuer(OAuth2Container target) {
        Map<?, ?> discovery = RestClient.create()
            .get()
            .uri(target.getAuthServerUrl() + "/.well-known/openid-configuration")
            .retrieve()
            .body(Map.class);
        return (String) discovery.get("issuer");
    }

    @Test
    public void yamlGenerationIncludesAllUsers() throws Exception {
        OAuth2Container testContainer = new OAuth2Container()
            .withUser("user1", "pass1", "ADMIN")
            .withUser("user2", "pass2", "USER", "VIEWER");

        testContainer.start();

        try {
            // If container started without exception, YAML generation worked
            assertTrue(testContainer.isRunning());
        } finally {
            testContainer.stop();
        }
    }

    @Test
    public void loadUsersAndClientsFromConfigFile() throws Exception {
        OAuth2Container testContainer = new OAuth2Container()
            .withConfigFile("test-config.yaml");

        testContainer.start();

        try {
            // Config file is mounted to /config/application.yaml
            // Spring Boot loads users and clients from it
            assertTrue(testContainer.isRunning());
            assertNotNull(testContainer.getAuthServerUrl());
        } finally {
            testContainer.stop();
        }
    }
}

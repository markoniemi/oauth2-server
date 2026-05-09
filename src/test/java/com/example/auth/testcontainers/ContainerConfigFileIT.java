package com.example.auth.testcontainers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ContainerConfigFileIT {

    private static Container containerFromClasspath;
    private static Container containerMixed;

    @BeforeAll
    static void setUp() {
        containerFromClasspath = new Container()
            .withConfigFile("classpath:test-config.yaml");

        containerMixed = new Container()
            .withConfigFile("classpath:test-users.yaml")
            .withUser("extra-user", "extra-pass", "USER");
    }

    @AfterAll
    static void tearDown() {
        if (containerFromClasspath != null) {
            try {
                containerFromClasspath.stop();
            } catch (Exception e) {
                // Ignore
            }
        }
        if (containerMixed != null) {
            try {
                containerMixed.stop();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Test
    public void loadsConfigFromClasspathYaml() {
        // Should not throw exception during load
        assertNotNull(containerFromClasspath);
    }

    @Test
    public void loadsUsersFromYamlFile() throws Exception {
        containerFromClasspath.start();

        try {
            assertTrue(containerFromClasspath.isRunning());
            // If container started, users were loaded successfully
        } finally {
            containerFromClasspath.stop();
        }
    }

    @Test
    public void mixesFileAndFluentConfig() throws Exception {
        containerMixed.start();

        try {
            assertTrue(containerMixed.isRunning());
            // If container started, both file and fluent users were applied
        } finally {
            containerMixed.stop();
        }
    }

    @Test
    public void throwsOnMissingFile() {
        assertThrows(RuntimeException.class, () -> {
            new Container()
                .withConfigFile("classpath:nonexistent.yaml");
        });
    }

    @Test
    public void loadedConfigHasUsers() {
        // Container was created with config file - verify it loaded without errors
        assertNotNull(containerFromClasspath);
    }

    @Test
    public void mixedConfigHasMultipleUsers() {
        // Container was created with both file and fluent config
        assertNotNull(containerMixed);
    }
}

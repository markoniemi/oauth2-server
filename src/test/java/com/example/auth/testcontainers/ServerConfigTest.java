package com.example.auth.testcontainers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ServerConfigTest {

    @Test
    public void noDuplicateUsernames() {
        User user1 = new User("admin", "pass1", Set.of("ADMIN"));
        User user2 = new User("admin", "pass2", Set.of("USER"));

        assertThrows(IllegalArgumentException.class, () -> {
            new ServerConfig(
                List.of(user1, user2),
                List.of(),
                null,
                null
            );
        });
    }

    @Test
    public void noDuplicateClientIds() {
        Client client1 = new Client("app1", "secret1");
        Client client2 = new Client("app1", "secret2");

        assertThrows(IllegalArgumentException.class, () -> {
            new ServerConfig(
                List.of(),
                List.of(client1, client2),
                null,
                null
            );
        });
    }

    @Test
    public void validConfigCreatesSuccessfully() {
        User user = new User("admin", "password", Set.of("ADMIN"));
        Client client = new Client("app", "secret")
            .withRedirectUris("http://localhost:3000/callback")
            .withScopes("openid");

        ServerConfig config = new ServerConfig(
            List.of(user),
            List.of(client),
            "http://auth:9000",
            "/"
        );

        assertEquals(1, config.getUsers().size());
        assertEquals(1, config.getClients().size());
        assertEquals("http://auth:9000", config.getIssuerUrl());
        assertEquals("/", config.getContextPath());
    }

    @Test
    public void usersAreImmutable() {
        List<User> users = new ArrayList<>(
            List.of(new User("admin", "password", Set.of("ADMIN")))
        );
        ServerConfig config = new ServerConfig(users, List.of(), null, null);

        // Try to modify original list
        users.add(new User("user", "password", Set.of("USER")));

        // Config should still have only 1 user
        assertEquals(1, config.getUsers().size());
    }
}

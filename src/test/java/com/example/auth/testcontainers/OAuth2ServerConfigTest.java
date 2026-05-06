package com.example.auth.testcontainers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OAuth2ServerConfigTest {

    @Test
    public void noDuplicateUsernames() {
        OAuth2User user1 = new OAuth2User("admin", "pass1", Set.of("ADMIN"));
        OAuth2User user2 = new OAuth2User("admin", "pass2", Set.of("USER"));

        assertThrows(IllegalArgumentException.class, () -> {
            new OAuth2ServerConfig(
                List.of(user1, user2),
                List.of(),
                null,
                null
            );
        });
    }

    @Test
    public void noDuplicateClientIds() {
        OAuth2Client client1 = new OAuth2Client("app1", "secret1");
        OAuth2Client client2 = new OAuth2Client("app1", "secret2");

        assertThrows(IllegalArgumentException.class, () -> {
            new OAuth2ServerConfig(
                List.of(),
                List.of(client1, client2),
                null,
                null
            );
        });
    }

    @Test
    public void validConfigCreatesSuccessfully() {
        OAuth2User user = new OAuth2User("admin", "password", Set.of("ADMIN"));
        OAuth2Client client = new OAuth2Client("app", "secret")
            .withRedirectUri("http://localhost:3000/callback")
            .withScopes("openid");

        OAuth2ServerConfig config = new OAuth2ServerConfig(
            List.of(user),
            List.of(client),
            "http://auth:9000",
            "/"
        );

        assertEquals(1, config.users().size());
        assertEquals(1, config.clients().size());
        assertEquals("http://auth:9000", config.issuerUrl());
        assertEquals("/", config.contextPath());
    }

    @Test
    public void usersAreImmutable() {
        List<OAuth2User> users = new ArrayList<>(
            List.of(new OAuth2User("admin", "password", Set.of("ADMIN")))
        );
        OAuth2ServerConfig config = new OAuth2ServerConfig(users, List.of(), null, null);

        // Try to modify original list
        users.add(new OAuth2User("user", "password", Set.of("USER")));

        // Config should still have only 1 user
        assertEquals(1, config.users().size());
    }
}

package com.example.auth.testcontainers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Set;

public class ContainerBuilderTest {

    @Test
    public void builderAccumulatesUsers() {
        ServerConfig config = ServerConfig.builder()
            .users(List.of(
                new User("admin", "password", Set.of("ADMIN", "USER")),
                new User("viewer", "pass123", Set.of("VIEWER"))
            ))
            .build();

        assertEquals(2, config.getUsers().size());
    }

    @Test
    public void builderAccumulatesClients() {
        ServerConfig config = ServerConfig.builder()
            .clients(List.of(
                new Client("app1", "secret1")
                    .withRedirectUris("http://localhost:3000/callback")
                    .withScopes("openid"),
                new Client("app2", "secret2")
                    .withRedirectUris("http://localhost:4000/callback")
                    .withScopes("profile")
            ))
            .build();

        assertEquals(2, config.getClients().size());
    }

    @Test
    public void builderWithCustomIssuerUrl() {
        ServerConfig config = ServerConfig.builder()
            .users(List.of(new User("admin", "password", Set.of("ADMIN"))))
            .issuerUrl("http://custom-auth:9000")
            .build();

        assertEquals("http://custom-auth:9000", config.getIssuerUrl());
    }

    @Test
    public void builderWithContextPath() {
        ServerConfig config = ServerConfig.builder()
            .users(List.of(new User("admin", "password", Set.of("ADMIN"))))
            .contextPath("/auth")
            .build();

        assertEquals("/auth", config.getContextPath());
    }

    @Test
    public void builderValidatesOnBuild() {
        User user1 = new User("admin", "pass1", Set.of("ADMIN"));
        User user2 = new User("admin", "pass2", Set.of("USER"));

        assertThrows(IllegalArgumentException.class, () -> {
            ServerConfig.builder()
                .users(List.of(user1, user2))
                .build();
        });
    }
}

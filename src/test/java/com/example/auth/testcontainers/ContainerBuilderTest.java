package com.example.auth.testcontainers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Set;

public class ContainerBuilderTest {

    @Test
    public void builderAccumulatesUsers() {
        ServerConfigBuilder builder = new ServerConfigBuilder()
            .withUser("admin", "password", "ADMIN", "USER")
            .withUser("viewer", "pass123", "VIEWER");

        ServerConfig config = builder.build();
        assertEquals(2, config.users().size());
    }

    @Test
    public void builderAccumulatesClients() {
        ServerConfigBuilder builder = new ServerConfigBuilder()
            .withOAuth2Client(
                new Client("app1", "secret1")
                    .withRedirectUri("http://localhost:3000/callback")
                    .withScopes("openid")
            )
            .withOAuth2Client(
                new Client("app2", "secret2")
                    .withRedirectUri("http://localhost:4000/callback")
                    .withScopes("profile")
            );

        ServerConfig config = builder.build();
        assertEquals(2, config.clients().size());
    }

    @Test
    public void builderWithCustomIssuerUrl() {
        ServerConfig config = new ServerConfigBuilder()
            .withUser("admin", "password", "ADMIN")
            .withIssuerUrl("http://custom-auth:9000")
            .build();

        assertEquals("http://custom-auth:9000", config.issuerUrl());
    }

    @Test
    public void builderWithContextPath() {
        ServerConfig config = new ServerConfigBuilder()
            .withUser("admin", "password", "ADMIN")
            .withContextPath("/auth")
            .build();

        assertEquals("/auth", config.contextPath());
    }

    @Test
    public void builderValidatesOnBuild() {
        User user1 = new User("admin", "pass1", Set.of("ADMIN"));
        User user2 = new User("admin", "pass2", Set.of("USER"));

        ServerConfigBuilder builder = new ServerConfigBuilder();
        builder.addUser(user1);
        builder.addUser(user2);

        assertThrows(IllegalArgumentException.class, builder::build);
    }
}

package com.example.auth.testcontainers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Set;

public class OAuth2ContainerBuilderTest {

    @Test
    public void builderAccumulatesUsers() {
        OAuth2ServerConfigBuilder builder = new OAuth2ServerConfigBuilder()
            .withUser("admin", "password", "ADMIN", "USER")
            .withUser("viewer", "pass123", "VIEWER");

        OAuth2ServerConfig config = builder.build();
        assertEquals(2, config.users().size());
    }

    @Test
    public void builderAccumulatesClients() {
        OAuth2ServerConfigBuilder builder = new OAuth2ServerConfigBuilder()
            .withOAuth2Client(
                new OAuth2Client("app1", "secret1")
                    .withRedirectUri("http://localhost:3000/callback")
                    .withScopes("openid")
            )
            .withOAuth2Client(
                new OAuth2Client("app2", "secret2")
                    .withRedirectUri("http://localhost:4000/callback")
                    .withScopes("profile")
            );

        OAuth2ServerConfig config = builder.build();
        assertEquals(2, config.clients().size());
    }

    @Test
    public void builderWithCustomIssuerUrl() {
        OAuth2ServerConfig config = new OAuth2ServerConfigBuilder()
            .withUser("admin", "password", "ADMIN")
            .withIssuerUrl("http://custom-auth:9000")
            .build();

        assertEquals("http://custom-auth:9000", config.issuerUrl());
    }

    @Test
    public void builderWithContextPath() {
        OAuth2ServerConfig config = new OAuth2ServerConfigBuilder()
            .withUser("admin", "password", "ADMIN")
            .withContextPath("/auth")
            .build();

        assertEquals("/auth", config.contextPath());
    }

    @Test
    public void builderValidatesOnBuild() {
        OAuth2User user1 = new OAuth2User("admin", "pass1", Set.of("ADMIN"));
        OAuth2User user2 = new OAuth2User("admin", "pass2", Set.of("USER"));

        OAuth2ServerConfigBuilder builder = new OAuth2ServerConfigBuilder();
        builder.addUser(user1);
        builder.addUser(user2);

        assertThrows(IllegalArgumentException.class, builder::build);
    }
}

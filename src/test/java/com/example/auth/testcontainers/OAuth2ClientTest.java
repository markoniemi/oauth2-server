package com.example.auth.testcontainers;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class OAuth2ClientTest {

    @Test
    public void clientIdCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OAuth2Client("", "secret");
        });
    }

    @Test
    public void clientSecretCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OAuth2Client("my-app", "");
        });
    }

    @Test
    public void fluentBuilderWithRedirectUri() {
        OAuth2Client client = new OAuth2Client("my-app", "secret")
            .withRedirectUri("http://localhost:3000/callback");

        assertEquals("my-app", client.getClientId());
        assertEquals("secret", client.getClientSecret());
        assertTrue(client.getRedirectUris().contains("http://localhost:3000/callback"));
    }

    @Test
    public void multipleRedirectUris() {
        OAuth2Client client = new OAuth2Client("my-app", "secret")
            .withRedirectUri("http://localhost:3000/callback")
            .withRedirectUri("http://localhost:3000/logout");

        assertEquals(2, client.getRedirectUris().size());
    }

    @Test
    public void fluentBuilderWithScopes() {
        OAuth2Client client = new OAuth2Client("my-app", "secret")
            .withScopes("openid", "profile", "email");

        assertEquals(3, client.getScopes().size());
        assertTrue(client.getScopes().containsAll(Set.of("openid", "profile", "email")));
    }

    @Test
    public void defaultGrantTypes() {
        OAuth2Client client = new OAuth2Client("my-app", "secret");
        assertEquals(Set.of("authorization_code", "refresh_token"), client.getGrantTypes());
    }

    @Test
    public void customGrantTypes() {
        OAuth2Client client = new OAuth2Client("my-app", "secret")
            .withGrantTypes("client_credentials");

        assertEquals(Set.of("client_credentials"), client.getGrantTypes());
    }
}

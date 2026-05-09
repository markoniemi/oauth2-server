package com.example.auth.testcontainers;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class ClientTest {

    @Test
    public void clientIdCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Client("", "secret");
        });
    }

    @Test
    public void clientSecretCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Client("my-app", "");
        });
    }

    @Test
    public void fluentBuilderWithRedirectUri() {
        Client client = new Client("my-app", "secret")
            .withRedirectUri("http://localhost:3000/callback");

        assertEquals("my-app", client.getClientId());
        assertEquals("secret", client.getClientSecret());
        assertTrue(client.getRedirectUris().contains("http://localhost:3000/callback"));
    }

    @Test
    public void multipleRedirectUris() {
        Client client = new Client("my-app", "secret")
            .withRedirectUri("http://localhost:3000/callback")
            .withRedirectUri("http://localhost:3000/logout");

        assertEquals(2, client.getRedirectUris().size());
    }

    @Test
    public void fluentBuilderWithScopes() {
        Client client = new Client("my-app", "secret")
            .withScopes("openid", "profile", "email");

        assertEquals(3, client.getScopes().size());
        assertTrue(client.getScopes().containsAll(Set.of("openid", "profile", "email")));
    }

    @Test
    public void defaultGrantTypes() {
        Client client = new Client("my-app", "secret");
        assertEquals(Set.of("authorization_code", "refresh_token"), client.getGrantTypes());
    }

    @Test
    public void customGrantTypes() {
        Client client = new Client("my-app", "secret")
            .withGrantTypes("client_credentials");

        assertEquals(Set.of("client_credentials"), client.getGrantTypes());
    }
}

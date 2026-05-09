package com.example.auth.testcontainers;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static jakarta.validation.Validation.buildDefaultValidatorFactory;
import static org.junit.jupiter.api.Assertions.*;

public class ClientTest {

    private static final ValidatorFactory factory = buildDefaultValidatorFactory();
    private static final Validator validator = factory.getValidator();

    @Test
    public void clientIdCannotBeBlank() {
        Client client = new Client("", "secret");
        Set<ConstraintViolation<Client>> violations = validator.validate(client);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("clientId")));
    }

    @Test
    public void clientSecretCannotBeBlank() {
        Client client = new Client("my-app", "");
        Set<ConstraintViolation<Client>> violations = validator.validate(client);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("clientSecret")));
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

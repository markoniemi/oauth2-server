package com.example.auth.testcontainers;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class OAuth2UserTest {

    @Test
    public void usernameCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OAuth2User("", "password", Set.of("USER"));
        });
    }

    @Test
    public void passwordCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OAuth2User("admin", "", Set.of("USER"));
        });
    }

    @Test
    public void rolesCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OAuth2User("admin", "password", Set.of());
        });
    }

    @Test
    public void validUserCreatesSuccessfully() {
        OAuth2User user = new OAuth2User("admin", "password", Set.of("ADMIN", "USER"));
        assertEquals("admin", user.username());
        assertEquals("password", user.password());
        assertEquals(Set.of("ADMIN", "USER"), user.roles());
    }

    @Test
    public void rolesAreImmutable() {
        Set<String> original = new HashSet<>(Set.of("USER"));
        OAuth2User user = new OAuth2User("admin", "password", original);

        // Modify original, should not affect user
        original.add("ADMIN");
        assertEquals(Set.of("USER"), user.roles());
    }
}

package com.example.auth.testcontainers;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;

import static jakarta.validation.Validation.buildDefaultValidatorFactory;
import static org.junit.jupiter.api.Assertions.*;

public class UserTest {
    private static final ValidatorFactory factory = buildDefaultValidatorFactory();
    private static final Validator validator = factory.getValidator();

    @Test
    public void usernameCannotBeBlank() {
        User user = new User("", "password", Set.of("USER"));
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty(), "Should have validation violations for blank username");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("username")));
    }

    @Test
    public void passwordCannotBeBlank() {
        User user = new User("admin", "", Set.of("USER"));
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty(), "Should have validation violations for blank password");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    public void rolesCannotBeEmpty() {
        User user = new User("admin", "password", Set.of());
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty(), "Should have validation violations for empty roles");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("roles")));
    }

    @Test
    public void validUserCreatesSuccessfully() {
        User user = new User("admin", "password", Set.of("ADMIN", "USER"));
        assertEquals("admin", user.getUsername());
        assertEquals("password", user.getPassword());
        assertEquals(Set.of("ADMIN", "USER"), user.getRoles());
    }

    @Test
    public void rolesAreImmutable() {
        Set<String> original = new HashSet<>(Set.of("USER"));
        User user = new User("admin", "password", original);

        // Modify original, should not affect user
        original.add("ADMIN");
        assertEquals(Set.of("USER"), user.getRoles());
    }
}

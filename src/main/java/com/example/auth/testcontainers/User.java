package com.example.auth.testcontainers;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record User(
    @NotBlank(message = "Username cannot be blank")
    String username,

    @NotBlank(message = "Password cannot be blank")
    String password,

    @NotEmpty(message = "Roles cannot be empty")
    Set<String> roles
) {
    public User {
        if (username != null && username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
        if (password != null && password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be blank");
        }
        if (roles != null && roles.isEmpty()) {
            throw new IllegalArgumentException("Roles cannot be empty");
        }
        roles = Set.copyOf(roles);  // immutable defensive copy
    }
}

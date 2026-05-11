package com.example.auth.testcontainers;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Data
@RequiredArgsConstructor
public class User {
    @NotBlank(message = "Username cannot be blank")
    private final String username;

    @NotBlank(message = "Password cannot be blank")
    private final String password;

    @NotEmpty(message = "Roles cannot be empty")
    private final Set<String> roles;
}

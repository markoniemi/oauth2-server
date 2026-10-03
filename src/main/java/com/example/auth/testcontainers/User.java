package com.example.auth.testcontainers;

import java.util.Set;

/** A login user passed to the container as {@code app.security.users}. */
public record User(String username, String password, Set<String> roles) {

  public User {
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("Username cannot be blank");
    }
    if (password == null || password.isBlank()) {
      throw new IllegalArgumentException("Password cannot be blank");
    }
    if (roles == null || roles.isEmpty()) {
      throw new IllegalArgumentException("Roles cannot be empty");
    }
    roles = Set.copyOf(roles);
  }

  @Override
  public String toString() {
    return "User[username=" + username + ", roles=" + roles + "]";
  }
}

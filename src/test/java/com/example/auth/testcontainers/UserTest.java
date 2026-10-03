package com.example.auth.testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void usernameCannotBeBlank() {
    assertThrows(IllegalArgumentException.class, () -> new User("", "password", Set.of("USER")));
  }

  @Test
  public void passwordCannotBeBlank() {
    assertThrows(IllegalArgumentException.class, () -> new User("admin", "", Set.of("USER")));
  }

  @Test
  public void rolesCannotBeEmpty() {
    assertThrows(IllegalArgumentException.class, () -> new User("admin", "password", Set.of()));
  }

  @Test
  public void validUserCreatesSuccessfully() {
    User user = new User("admin", "password", Set.of("ADMIN", "USER"));
    assertEquals("admin", user.username());
    assertEquals("password", user.password());
    assertEquals(Set.of("ADMIN", "USER"), user.roles());
  }

  @Test
  public void rolesAreCopied() {
    Set<String> roles = new HashSet<>(Set.of("USER"));
    User user = new User("admin", "password", roles);
    roles.add("ADMIN");
    assertEquals(Set.of("USER"), user.roles());
  }
}

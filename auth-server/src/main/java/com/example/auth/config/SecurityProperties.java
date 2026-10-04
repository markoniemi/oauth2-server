package com.example.auth.config;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Users that can log in, bound from {@code app.security.users}. */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(List<User> users) {

  public SecurityProperties {
    users = emptyIfNull(users);
  }

  /**
   * A login user. The password is plain text or prefixed with an encoding id, e.g. {@code
   * {bcrypt}...}.
   */
  public record User(String username, String password, List<String> roles) {

    public User {
      roles = emptyIfNull(roles);
    }

    @Override
    public String toString() {
      return "User[username=" + username + ", roles=" + roles + "]";
    }
  }
}

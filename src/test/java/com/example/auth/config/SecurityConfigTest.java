package com.example.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class SecurityConfigTest {

  private final SecurityConfig securityConfig = new SecurityConfig();
  private final PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

  @Test
  void plainValuesMatchAsPlainText() {
    // Plain user passwords and client secrets from YAML keep working
    assertThat(passwordEncoder.matches("admin", "admin")).isTrue();
    assertThat(passwordEncoder.matches("wrong", "admin")).isFalse();
  }

  @Test
  void encodedValuesMatch() {
    String hash = "{bcrypt}" + new BCryptPasswordEncoder().encode("secret");

    assertThat(passwordEncoder.matches("secret", hash)).isTrue();
    assertThat(passwordEncoder.matches("secret", "{noop}secret")).isTrue();
  }

  @Test
  void configuredPasswordIsUsedAsIs() {
    String hash = "{bcrypt}" + new BCryptPasswordEncoder().encode("secret");

    UserDetails user = load(new SecurityProperties.User("admin", hash, List.of("USER")));

    assertThat(user.getPassword()).isEqualTo(hash);
  }

  @Test
  void rolesBecomeAuthorities() {
    UserDetails user = load(new SecurityProperties.User("admin", "admin", List.of("USER", "ADMIN")));

    assertThat(user.getAuthorities()).extracting(Object::toString)
        .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
  }

  @Test
  void userToStringHidesPassword() {
    assertThat(new SecurityProperties.User("admin", "top-secret", List.of("USER")).toString())
        .doesNotContain("top-secret");
  }

  private UserDetails load(SecurityProperties.User user) {
    return securityConfig.userDetailsService(new SecurityProperties(List.of(user)))
        .loadUserByUsername(user.username());
  }
}

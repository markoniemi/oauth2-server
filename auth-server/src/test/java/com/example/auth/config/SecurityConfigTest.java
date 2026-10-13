package com.example.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

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

  @Test
  void accessTokenGetsRolesClaim() {
    JwtEncodingContext context =
        encodingContext(OAuth2TokenType.ACCESS_TOKEN, "ROLE_ADMIN", "ROLE_USER");

    securityConfig.rolesClaimCustomizer().customize(context);

    assertThat(context.getClaims().build().<Object>getClaim("roles"))
        .isEqualTo(Set.of("ROLE_ADMIN", "ROLE_USER"));
  }

  @Test
  void userWithoutAdminRoleGetsNoAdminClaim() {
    JwtEncodingContext context = encodingContext(OAuth2TokenType.ACCESS_TOKEN, "ROLE_USER");

    securityConfig.rolesClaimCustomizer().customize(context);

    assertThat(context.getClaims().build().<Object>getClaim("roles"))
        .isEqualTo(Set.of("ROLE_USER"));
  }

  @Test
  void refreshTokenGetsNoRolesClaim() {
    JwtEncodingContext context = encodingContext(OAuth2TokenType.REFRESH_TOKEN, "ROLE_ADMIN");

    securityConfig.rolesClaimCustomizer().customize(context);

    assertThat(context.getClaims().build().hasClaim("roles")).isFalse();
  }

  private static JwtEncodingContext encodingContext(OAuth2TokenType type, String... authorities) {
    return JwtEncodingContext.with(
            JwsHeader.with(SignatureAlgorithm.RS256), JwtClaimsSet.builder().subject("u"))
        .tokenType(type)
        .principal(new UsernamePasswordAuthenticationToken(
            "u", "p", AuthorityUtils.createAuthorityList(authorities)))
        .build();
  }

  private UserDetails load(SecurityProperties.User user) {
    return securityConfig.userDetailsService(new SecurityProperties(List.of(user)))
        .loadUserByUsername(user.username());
  }
}

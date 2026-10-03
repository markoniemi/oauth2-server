package com.example.auth.config;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {
  @Bean
  @Order(1)
  public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
      throws Exception {
    http.oauth2AuthorizationServer((authorizationServer) -> {
          // Match all authorization server endpoints, including OIDC /connect/logout and /userinfo
          http.securityMatcher(authorizationServer.getEndpointsMatcher());
          authorizationServer.oidc(Customizer.withDefaults());
        })
        // Require an authenticated user so anonymous authorization requests trigger the login entry point
        .authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated())
        .cors(Customizer.withDefaults())
        // Redirect to the login page when not authenticated from the authorization endpoint
        .exceptionHandling(
            (exceptions) ->
                exceptions.defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"),
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
        // Accept access tokens for User Info and/or Client Registration
        .oauth2ResourceServer((resourceServer) -> resourceServer.jwt(Customizer.withDefaults()));

    return http.build();
  }

  @Bean
  @Order(2)
  public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
    http.cors(Customizer.withDefaults())
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/actuator/**").permitAll()
            .anyRequest().authenticated())
        .formLogin(Customizer.withDefaults());
    return http.build();
  }

  @Bean
  public UserDetailsService userDetailsService(SecurityProperties securityProperties) {
    List<UserDetails> users = securityProperties.users().stream()
        .map(u -> User.withUsername(u.username())
            .password(u.password())
            .roles(u.roles().toArray(new String[0]))
            .build())
        .toList();

    log.debug("Users ({}): {}", users.size(), securityProperties.users());

    return new InMemoryUserDetailsManager(users);
  }

  /**
   * Checks user passwords and client secrets. Values may be encoded with an id prefix such as
   * {@code {bcrypt}...}; values without a prefix are compared as plain text.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    DelegatingPasswordEncoder encoder =
        (DelegatingPasswordEncoder) PasswordEncoderFactories.createDelegatingPasswordEncoder();
    encoder.setDefaultPasswordEncoderForMatches(NoOpPasswordEncoder.getInstance());
    return encoder;
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
    log.debug("CORS allowed origins: {}", corsProperties.allowedOrigins());

    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(corsProperties.allowedOrigins());
    configuration.setAllowedMethods(List.of("*"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}

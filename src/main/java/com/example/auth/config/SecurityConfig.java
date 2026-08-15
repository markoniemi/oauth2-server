package com.example.auth.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.auth.testcontainers.Client;
import com.example.auth.testcontainers.ClientConfig;

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
import org.springframework.security.oauth2.server.authorization.web.OAuth2TokenRevocationEndpointFilter;
import org.springframework.security.oauth2.server.authorization.web.OAuth2AuthorizationServerMetadataEndpointFilter;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
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
    http.securityMatcher("/oauth2/**", "/.well-known/**");
    http.oauth2AuthorizationServer((authorizationServer) ->
        authorizationServer.oidc(Customizer.withDefaults())
    );
    http.cors(Customizer.withDefaults())
        // Redirect to the login page when not authenticated from the
        // authorization endpoint
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
  public UserDetailsService userDetailsService(SecurityProperties securityProperties,
      PasswordEncoder passwordEncoder) {
    List<UserDetails> users = securityProperties.getUsers().stream()
        .map(u -> User.builder()
            .username(u.getUsername())
            .password(passwordEncoder.encode(u.getPassword()))
            .roles(u.getRoles().toArray(new String[0]))
            .build())
        .collect(Collectors.toList());

    log.debug("Users ({}): {}", securityProperties.getUsers().size(), securityProperties.getUsers());

    return new InMemoryUserDetailsManager(users);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return NoOpPasswordEncoder.getInstance();
  }

  @Bean
  public HttpFirewall httpFirewall() {
    StrictHttpFirewall firewall = new StrictHttpFirewall();
    firewall.setAllowSemicolon(true);
    return firewall;
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    Set<String> origins = new HashSet<>();

    List<Client> clients = ClientConfig.getClients();
    if (clients != null) {
      clients.forEach(client ->
          client.getRedirectUris().forEach(uri -> {
            try {
              origins.add(new java.net.URI(uri).getScheme() + "://" + new java.net.URI(uri).getAuthority());
            } catch (java.net.URISyntaxException e) {
              origins.add(uri);
            }
          })
      );
    }

    if (origins.isEmpty()) {
      origins.add("http://localhost:8080");
      origins.add("http://localhost:5173");
    }

    log.debug("CORS allowed origins: {}", origins);

    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(new ArrayList<>(origins));
    configuration.setAllowedMethods(List.of("*"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}

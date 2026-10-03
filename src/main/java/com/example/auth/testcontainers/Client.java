package com.example.auth.testcontainers;

import jakarta.validation.constraints.NotBlank;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@RequiredArgsConstructor
public class Client {
    @NotBlank(message = "Client ID cannot be blank")
    private final String clientId;

    private final String clientSecret;

    private Set<String> redirectUris = new HashSet<>();
    private Set<String> postLogoutRedirectUris = new HashSet<>();
    private Set<String> scopes = new HashSet<>();
    private Set<String> grantTypes = new HashSet<>(Set.of("authorization_code", "refresh_token"));
    private String tokenEndpointAuthMethod = "client_secret_basic";
    private boolean requireProofKey = false;
    private Duration accessTokenTimeToLive;
    private Duration refreshTokenTimeToLive;

    /** A client without a secret is public: it authenticates with PKCE instead of a secret. */
    public boolean isPublicClient() {
        return clientSecret == null || clientSecret.isEmpty();
    }

    public Client withRedirectUris(String... uris) {
        this.redirectUris=new HashSet<>(Arrays.asList(uris));
        return this;
    }

    public Client withPostLogoutRedirectUris(String... uris) {
        this.postLogoutRedirectUris = new HashSet<>(Arrays.asList(uris));
        return this;
    }

    public Client withAccessTokenTimeToLive(Duration timeToLive) {
        this.accessTokenTimeToLive = timeToLive;
        return this;
    }

    public Client withRefreshTokenTimeToLive(Duration timeToLive) {
        this.refreshTokenTimeToLive = timeToLive;
        return this;
    }

    public Client withScopes(String... scopes) {
        this.scopes=new HashSet<>(Arrays.asList(scopes));
        return this;
    }

    public Client withGrantTypes(String... types) {
        this.grantTypes=new HashSet<>(Arrays.asList(types));
        return this;
    }

    public Client withTokenEndpointAuthMethod(String method) {
        this.tokenEndpointAuthMethod = method;
        return this;
    }

    public Client withRequireProofKey(boolean requireProofKey) {
        this.requireProofKey = requireProofKey;
        return this;
    }
}

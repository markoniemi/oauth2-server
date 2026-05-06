package com.example.auth.testcontainers;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Set;

@Data
public class OAuth2Client {
    @NotBlank(message = "Client ID cannot be blank")
    private final String clientId;

    @NotBlank(message = "Client secret cannot be blank")
    private final String clientSecret;

    private Set<String> redirectUris = new HashSet<>();
    private Set<String> scopes = new HashSet<>();
    private Set<String> grantTypes = new HashSet<>(Set.of("authorization_code", "refresh_token"));

    public OAuth2Client(String clientId, String clientSecret) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("Client ID cannot be blank");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("Client secret cannot be blank");
        }
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public OAuth2Client withRedirectUri(String... uris) {
        for (String uri : uris) {
            this.redirectUris.add(uri);
        }
        return this;
    }

    public OAuth2Client withScopes(String... scopeList) {
        this.scopes.clear();
        for (String scope : scopeList) {
            this.scopes.add(scope);
        }
        return this;
    }

    public OAuth2Client withGrantTypes(String... types) {
        this.grantTypes.clear();
        for (String type : types) {
            this.grantTypes.add(type);
        }
        return this;
    }
}

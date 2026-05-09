package com.example.auth.testcontainers;

import lombok.Builder;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import java.util.HashSet;
import java.util.Set;

@Data
@Validated
@RequiredArgsConstructor
public class Client {
    @NotBlank(message = "Client ID cannot be blank")
    private final String clientId;

    @NotBlank(message = "Client secret cannot be blank")
    private final String clientSecret;

    private Set<String> redirectUris = new HashSet<>();
    private Set<String> scopes = new HashSet<>();
    private Set<String> grantTypes = new HashSet<>(Set.of("authorization_code", "refresh_token"));

    public Client withRedirectUri(String... uris) {
        for (String uri : uris) {
            this.redirectUris.add(uri);
        }
        return this;
    }

    public Client withScopes(String... scopeList) {
        this.scopes.clear();
        for (String scope : scopeList) {
            this.scopes.add(scope);
        }
        return this;
    }

    public Client withGrantTypes(String... types) {
        this.grantTypes.clear();
        for (String type : types) {
            this.grantTypes.add(type);
        }
        return this;
    }
}

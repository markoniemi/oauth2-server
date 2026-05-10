package com.example.auth.testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

@Configuration
public class ClientConfig {

    // Static field to hold clients provided by TestContainers
    private static List<Client> clients = new ArrayList<>();

    public static void setClients(List<Client> clients) {
        ClientConfig.clients = new ArrayList<>(clients);
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        List<RegisteredClient> registeredClients = new ArrayList<>();

        for (Client client : clients) {
            RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(client.getClientId())
                .clientSecret(client.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);

            // Add redirect URIs
            for (String uri : client.getRedirectUris()) {
                builder.redirectUri(uri);
            }

            // Add scopes
            for (String scope : client.getScopes()) {
                builder.scope(scope);
            }

            // Add grant types
            for (String grantType : client.getGrantTypes()) {
                if ("authorization_code".equals(grantType)) {
                    builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
                } else if ("refresh_token".equals(grantType)) {
                    builder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
                } else if ("client_credentials".equals(grantType)) {
                    builder.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);
                }
            }

            registeredClients.add(builder.build());
        }

        return new InMemoryRegisteredClientRepository(registeredClients);
    }
}

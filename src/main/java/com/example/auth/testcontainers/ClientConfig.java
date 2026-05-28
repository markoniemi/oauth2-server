package com.example.auth.testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

@Configuration
public class ClientConfig {

    private static List<Client> clients = null;

    public static void setClients(List<Client> clients) {
        ClientConfig.clients = new ArrayList<>(clients);
    }

    public static void clearClients() {
        ClientConfig.clients = null;
    }

    @Bean
    @Primary
    public RegisteredClientRepository testcontainersRegisteredClientRepository() {
        if (clients == null) {
            return new InMemoryRegisteredClientRepository();
        }
        List<RegisteredClient> registeredClients = new ArrayList<>();
        for (Client client : clients) {
            RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(client.getClientId())
                .clientSecret(client.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);

            for (String uri : client.getRedirectUris()) {
                builder.redirectUri(uri);
            }
            for (String scope : client.getScopes()) {
                builder.scope(scope);
            }
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

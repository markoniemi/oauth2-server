package com.example.auth.testcontainers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

@Slf4j
@Configuration
public class ClientConfig {

    private static List<Client> clients = null;

    public static void setClients(List<Client> clients) {
        ClientConfig.clients = new ArrayList<>(clients);
    }

    public static void clearClients() {
        ClientConfig.clients = null;
    }

    public static List<Client> getClients() {
        return clients;
    }

    @Bean
    public RegisteredClientRepository testcontainersRegisteredClientRepository() {
        if (clients != null) {
            log.debug("Clients ({}): {}", clients.size(), clients);
            return createFromTestContainersClients(clients);
        }
        RegisteredClient nativeClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("frontend-client")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost:8080")
            .redirectUri("http://localhost:5173")
            .postLogoutRedirectUri("http://localhost:5173")
            .postLogoutRedirectUri("http://localhost:8080")
            .scope("openid")
            .scope("profile")
            .scope("email")
            .clientSettings(ClientSettings.builder().requireProofKey(true).build())
            .tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofHours(1))
                .refreshTokenTimeToLive(Duration.ofDays(7))
                .build())
            .build();
        log.debug("Clients (1): [Client(clientId=frontend-client, ...)]");
        return new InMemoryRegisteredClientRepository(nativeClient);
    }

    private RegisteredClientRepository createFromTestContainersClients(List<Client> clientList) {
        List<RegisteredClient> registeredClients = new ArrayList<>();
        for (Client client : clientList) {
            RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(client.getClientId());

            boolean isPublicClient = client.getClientSecret() == null || client.getClientSecret().isEmpty();
            if (!isPublicClient) {
                builder.clientSecret(client.getClientSecret());
            }

            String authMethod = client.getTokenEndpointAuthMethod();
            if (isPublicClient && "client_secret_basic".equals(authMethod)) {
                authMethod = "none";
            }
            setClientAuthenticationMethod(builder, authMethod);

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

            if (client.isRequireProofKey() || isPublicClient) {
                builder.clientSettings(ClientSettings.builder().requireProofKey(true).build());
            }

            registeredClients.add(builder.build());
        }
        return new InMemoryRegisteredClientRepository(registeredClients);
    }

    private void setClientAuthenticationMethod(RegisteredClient.Builder builder, String authMethod) {
        if ("none".equals(authMethod)) {
            builder.clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
        } else if ("client_secret_post".equals(authMethod)) {
            builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST);
        } else {
            builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
        }
    }
}

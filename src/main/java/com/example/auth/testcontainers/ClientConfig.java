package com.example.auth.testcontainers;

import java.time.Duration;
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
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import com.example.auth.config.ClientProperties;

@Configuration
public class ClientConfig {

    private static List<Client> clients = null;

    public static void setClients(List<Client> clients) {
        ClientConfig.clients = new ArrayList<>(clients);
    }

    @Bean
    @Primary
    public RegisteredClientRepository registeredClientRepository(ClientProperties clientProperties) {
        List<RegisteredClient> registeredClients = new ArrayList<>();

        if (clients != null) {
            registeredClients.addAll(createFromTestContainersClients(clients));
        } else {
            registeredClients.addAll(createFromProperties(clientProperties));
        }

        return new InMemoryRegisteredClientRepository(registeredClients);
    }

    private List<RegisteredClient> createFromTestContainersClients(List<Client> clientList) {
        List<RegisteredClient> registeredClients = new ArrayList<>();

        for (Client client : clientList) {
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

        return registeredClients;
    }

    private List<RegisteredClient> createFromProperties(ClientProperties clientProperties) {
        List<RegisteredClient> registeredClients = new ArrayList<>();

        for (ClientProperties.ClientConfig clientConfig : clientProperties.getClients()) {
            RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientConfig.getClientId())
                .clientSecret(clientConfig.getClientSecret());

            ClientAuthenticationMethod authMethod = parseClientAuthenticationMethod(
                clientConfig.getClientAuthenticationMethod());
            builder.clientAuthenticationMethod(authMethod);

            for (String grantType : clientConfig.getAuthorizationGrantTypes()) {
                builder.authorizationGrantType(parseGrantType(grantType));
            }

            for (String uri : clientConfig.getRedirectUris()) {
                builder.redirectUri(uri);
            }

            for (String uri : clientConfig.getPostLogoutRedirectUris()) {
                builder.postLogoutRedirectUri(uri);
            }

            for (String scope : clientConfig.getScopes()) {
                builder.scope(scope);
            }

            builder.clientSettings(ClientSettings.builder()
                .requireProofKey(clientConfig.isRequireProofKey())
                .build());

            builder.tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofSeconds(clientConfig.getAccessTokenTimeToLive()))
                .refreshTokenTimeToLive(Duration.ofSeconds(clientConfig.getRefreshTokenTimeToLive()))
                .build());

            registeredClients.add(builder.build());
        }

        return registeredClients;
    }

    private ClientAuthenticationMethod parseClientAuthenticationMethod(String method) {
        if (method == null || "NONE".equals(method)) {
            return ClientAuthenticationMethod.NONE;
        }
        return ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
    }

    private AuthorizationGrantType parseGrantType(String grantType) {
        if ("authorization_code".equals(grantType)) {
            return AuthorizationGrantType.AUTHORIZATION_CODE;
        } else if ("refresh_token".equals(grantType)) {
            return AuthorizationGrantType.REFRESH_TOKEN;
        } else if ("client_credentials".equals(grantType)) {
            return AuthorizationGrantType.CLIENT_CREDENTIALS;
        }
        return AuthorizationGrantType.AUTHORIZATION_CODE;
    }
}

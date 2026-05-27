package com.example.auth.testcontainers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.auth.config.ClientProperties;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

@Disabled
public class ClientConfigTest {

    @AfterEach
    void tearDown() {
        ClientConfig.clearClients();
    }

    @Test
    void clearClientsRestoresPropertyBasedClients() {
        ClientConfig.setClients(List.of(
            new Client("test-client", "test-secret")
                .withRedirectUris("http://localhost:8080/callback")
                .withScopes("openid")));

        ClientProperties clientProperties = new ClientProperties();
        ClientProperties.ClientConfig propertyClient = new ClientProperties.ClientConfig();
        propertyClient.setClientId("frontend-client");
        propertyClient.setAuthorizationGrantTypes(List.of("authorization_code"));
        propertyClient.setRedirectUris(List.of("http://localhost:5173"));
        propertyClient.setScopes(List.of("openid"));
        clientProperties.setClients(List.of(propertyClient));

        ClientConfig clientConfig = new ClientConfig();
        RegisteredClientRepository customClientRepository =
            clientConfig.registeredClientRepository();
//        RegisteredClientRepository customClientRepository =
//            clientConfig.registeredClientRepository(clientProperties);
        assertNotNull(customClientRepository.findByClientId("test-client"));

        ClientConfig.clearClients();

        RegisteredClientRepository propertyClientRepository =
            clientConfig.registeredClientRepository();
//        RegisteredClientRepository propertyClientRepository =
//            clientConfig.registeredClientRepository(clientProperties);
        assertNotNull(propertyClientRepository.findByClientId("frontend-client"));
        assertNull(propertyClientRepository.findByClientId("test-client"));
    }
}

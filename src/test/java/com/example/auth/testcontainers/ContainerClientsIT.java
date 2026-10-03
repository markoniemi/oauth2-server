package com.example.auth.testcontainers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.net.HttpURLConnection;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerClientsIT {

    private static OAuth2Container container;

    @BeforeAll
    static void setUp() {
        container = new OAuth2Container()
            .withUser("admin", "admin123", "ADMIN")
            .withOAuth2Client(
                new Client("test-client", "test-secret")
                    .withRedirectUris("http://localhost:8080/callback")
                    .withScopes("openid", "profile", "api")
                    .withGrantTypes("authorization_code", "client_credentials")
            );
        container.start();
    }

    @AfterAll
    static void tearDown() {
        if (container != null) {
            container.stop();
        }
    }

    @Test
    public void clientCanObtainTokenViaClientCredentials() {
        RestClient restClient = RestClient.create();
        String tokenUrl = container.getAuthServerUrl() + "/oauth2/token";
        String encodedCredentials = Base64.getEncoder().encodeToString("test-client:test-secret".getBytes());

        var response = restClient.post()
            .uri(tokenUrl)
            .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials)
            .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
            .body("grant_type=client_credentials&scope=api")
            .retrieve()
            .toEntity(Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().get("access_token"));
    }

    @Test
    public void authorizationRequestForRegisteredClientRedirectsToLogin() {
        RestClient restClient = RestClient.builder()
            .requestFactory(new NoRedirectRequestFactory())
            .build();
        String authUrl = container.getAuthServerUrl() + "/oauth2/authorize"
            + "?client_id=test-client&response_type=code&scope=openid"
            + "&redirect_uri=http://localhost:8080/callback";

        var response = restClient.get()
            .uri(authUrl)
            .header(HttpHeaders.ACCEPT, "text/html")
            .retrieve()
            .toBodilessEntity();

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        String location = String.valueOf(response.getHeaders().getLocation());
        assertTrue(location.endsWith("/login"), location);
    }

    @Test
    public void containerIsRunningWithClient() {
        assertTrue(container.isRunning());
        assertNotNull(container.getAuthServerUrl());
    }

    private static class NoRedirectRequestFactory extends SimpleClientHttpRequestFactory {
        @Override
        protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws java.io.IOException {
            super.prepareConnection(connection, httpMethod);
            connection.setInstanceFollowRedirects(false);
        }
    }
}

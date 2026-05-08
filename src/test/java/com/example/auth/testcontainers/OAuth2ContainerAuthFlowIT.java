package com.example.auth.testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class OAuth2ContainerAuthFlowIT {

    private static OAuth2Container container;
    private WebClient webClient;

    @BeforeAll
    static void setUp() {
        container = new OAuth2Container()
            .withOAuth2Client(
                new OAuth2Client("test-frontend", "client-secret")
                    .withRedirectUri("http://localhost:8080/callback")
                    .withScopes("openid", "profile")
            );
        container.start();

        // Wait for container to be ready
        Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .ignoreExceptions()
            .until(() -> {
                try {
                    RestClient client = RestClient.create();
                    var response = client.get()
                        .uri(container.getAuthServerUrl() + "/.well-known/openid-configuration")
                        .retrieve()
                        .toEntity(String.class);
                    return response.getStatusCode().is2xxSuccessful();
                } catch (Exception e) {
                    return false;
                }
            });
    }

    @AfterAll
    static void tearDown() {
        if (container != null) {
            container.stop();
        }
    }

    @BeforeEach
    void setUpBeforeEach() {
        webClient = new WebClient();
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setRedirectEnabled(true);
        webClient.getCookieManager().clearCookies();
    }

    @Test
    public void authorizationEndpointRequiresAuthentication() throws Exception {
        String baseUrl = container.getAuthServerUrl();
        String authorizationUrl = baseUrl + "/oauth2/authorize?" +
            "response_type=code&" +
            "client_id=test-frontend&" +
            "scope=openid%20profile&" +
            "redirect_uri=http://localhost:8080/callback&" +
            "state=test-state";

        // Navigate to authorization endpoint without authentication
        Page page = webClient.getPage(authorizationUrl);
        assertInstanceOf(HtmlPage.class, page, "Should return HtmlPage for authorization endpoint");
        HtmlPage loginPage = (HtmlPage) page;

        // Should redirect to login page
        assertTrue(loginPage.getUrl().toString().contains("/login"),
            "Unauthenticated request should redirect to login page");
    }


    @Test
    public void discoveryEndpointIsAccessible() throws Exception {
        String baseUrl = container.getAuthServerUrl();
        RestClient restClient = RestClient.create();

        var response = restClient.get()
            .uri(baseUrl + "/.well-known/openid-configuration")
            .retrieve()
            .toEntity(String.class);

        assertEquals(200, response.getStatusCode().value(),
            "Discovery endpoint should be accessible");

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> discovery = mapper.readValue(response.getBody(), Map.class);

        assertTrue(discovery.containsKey("issuer"), "Should have issuer");
        assertTrue(discovery.containsKey("authorization_endpoint"), "Should have authorization_endpoint");
        assertTrue(discovery.containsKey("token_endpoint"), "Should have token_endpoint");
    }

}

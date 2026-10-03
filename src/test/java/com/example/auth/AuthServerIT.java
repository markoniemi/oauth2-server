package com.example.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Integration tests for OAuth2 authorization server.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthServerIT {

    @LocalServerPort
    private int port;

    private WebClient webClient;
    private String codeVerifier;
    private String codeChallenge;
    private String baseUrl;

    @BeforeEach
    public void setUp() throws NoSuchAlgorithmException {
        webClient = new WebClient();
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setRedirectEnabled(true);
        webClient.getCookieManager().clearCookies();

        baseUrl = "http://localhost:" + port;
        codeVerifier = PkceUtil.generateCodeVerifier();
        codeChallenge = PkceUtil.generateCodeChallenge(codeVerifier);
    }

    @Test
    public void performHealthCheck() throws Exception {
        Page response = webClient.getPage(baseUrl + "/actuator/health");
        assertThat(response.getWebResponse().getStatusCode()).isEqualTo(200);
    }

    @Test
    public void performDiscoveryCheck() throws Exception {
        Page response = webClient.getPage(baseUrl + "/.well-known/openid-configuration");
        assertThat(response.getWebResponse().getStatusCode()).isEqualTo(200);
    }

    @Test
    public void performAuthorizationRequestRedirectsToLogin() throws Exception {
        String authUrl = baseUrl + "/oauth2/authorize?" +
                "response_type=code&" +
                "client_id=frontend-client&" +
                "scope=openid&" +
                "redirect_uri=http://localhost:5173&" +
                "state=state&" +
                "nonce=nonce123&" +
                "code_challenge=" + codeChallenge + "&" +
                "code_challenge_method=S256";

        webClient.getOptions().setRedirectEnabled(false);
        Page response = webClient.getPage(authUrl);
        WebResponse webResponse = response.getWebResponse();

        assertThat(webResponse.getStatusCode()).isEqualTo(302);
        String location = webResponse.getResponseHeaderValue("Location");
        assertThat(location).contains("/login");
    }

    @Test
    public void performFullAuthenticationFlow() throws Exception {
        String authorizationRequestUri = baseUrl + "/oauth2/authorize?" +
                "response_type=code&" +
                "client_id=frontend-client&" +
                "scope=openid&" +
                "redirect_uri=http://localhost:5173&" +
                "state=state&" +
                "nonce=nonce123&" +
                "code_challenge=" + codeChallenge + "&" +
                "code_challenge_method=S256";

        webClient.getOptions().setRedirectEnabled(true);
        HtmlPage loginPage = webClient.getPage(authorizationRequestUri);

        assertThat(loginPage.getUrl().toString()).contains("/login");

        HtmlInput usernameInput = loginPage.querySelector("input[name='username']");
        HtmlInput passwordInput = loginPage.querySelector("input[name='password']");
        HtmlButton signInButton = loginPage.querySelector("button");

        usernameInput.type("admin");
        passwordInput.type("admin");

        webClient.getOptions().setRedirectEnabled(false);
        Page pageAfterLogin = signInButton.click();
        WebResponse responseAfterLogin = pageAfterLogin.getWebResponse();

        assertThat(responseAfterLogin.getStatusCode()).isEqualTo(302);
        String location = responseAfterLogin.getResponseHeaderValue("Location");

        Page authResponse = webClient.getPage(location);
        WebResponse finalResponse = authResponse.getWebResponse();

        assertThat(finalResponse.getStatusCode()).isEqualTo(302);
        String finalLocation = finalResponse.getResponseHeaderValue("Location");

        assertThat(finalLocation).startsWith("http://localhost:5173");
        assertThat(finalLocation).contains("code=");

        String code = finalLocation.substring(finalLocation.indexOf("code=") + 5);
        if (code.contains("&")) {
            code = code.substring(0, code.indexOf("&"));
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", "http://localhost:5173");
        form.add("client_id", "frontend-client");
        form.add("code_verifier", codeVerifier);

        String tokenResponse = restTemplate.postForObject(baseUrl + "/oauth2/token",
                new HttpEntity<>(form, headers), String.class);
        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> tokenMap = mapper.readValue(tokenResponse, Map.class);

        assertThat(tokenMap).containsKey("access_token");
        assertThat(tokenMap).containsKey("id_token");
        assertThat(tokenMap.get("token_type")).isEqualTo("Bearer");
    }
}

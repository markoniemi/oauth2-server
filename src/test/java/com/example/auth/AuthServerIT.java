package com.example.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class AuthServerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    private WebClient webClient;
    private String codeVerifier;
    private String codeChallenge;

    @BeforeEach
    public void setUp() throws NoSuchAlgorithmException {
        webClient = new WebClient();
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setRedirectEnabled(true);
        webClient.getCookieManager().clearCookies();

        codeVerifier = PkceUtil.generateCodeVerifier();
        codeChallenge = PkceUtil.generateCodeChallenge(codeVerifier);
    }

    @Test
    public void performHealthCheck() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    public void performDiscoveryCheck() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk());
    }

    @Test
    public void performAuthorizationRequestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/oauth2/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", "frontend-client")
                .queryParam("scope", "openid")
                .queryParam("redirect_uri", "http://localhost:5173")
                .queryParam("state", "state")
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "admin")
    public void performAuthorizationRequestReturnsCode() throws Exception {
        mockMvc.perform(get("/oauth2/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", "frontend-client")
                .queryParam("scope", "openid")
                .queryParam("redirect_uri", "http://localhost:5173")
                .queryParam("state", "state")
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    public void performFullAuthenticationFlow() throws Exception {
        String authorizationRequestUri = "http://localhost:" + port + "/oauth2/authorize?" +
                "response_type=code&" +
                "client_id=frontend-client&" +
                "scope=openid&" +
                "redirect_uri=http://localhost:5173&" +
                "state=state&" +
                "code_challenge=" + codeChallenge + "&" +
                "code_challenge_method=S256";

        // Enable redirects to reach the login page
        webClient.getOptions().setRedirectEnabled(true);
        HtmlPage loginPage = webClient.getPage(authorizationRequestUri);

        // Assert we are on the login page
        assertThat(loginPage.getUrl().toString()).contains("/login");

        HtmlInput usernameInput = loginPage.querySelector("input[name='username']");
        HtmlInput passwordInput = loginPage.querySelector("input[name='password']");
        HtmlButton signInButton = loginPage.querySelector("button");

        usernameInput.type("admin");
        passwordInput.type("admin");

        // Disable redirects to prevent attempting to connect to localhost:5173
        webClient.getOptions().setRedirectEnabled(false);

        // Submit the login form
        Page pageAfterLogin = signInButton.click();
        WebResponse responseAfterLogin = pageAfterLogin.getWebResponse();

        // The response should be a redirect back to the authorization endpoint
        assertThat(responseAfterLogin.getStatusCode()).isEqualTo(302);
        String location = responseAfterLogin.getResponseHeaderValue("Location");

        // Manually follow the redirect to the authorization endpoint
        Page authResponse = webClient.getPage(location);
        WebResponse finalResponse = authResponse.getWebResponse();

        // The authorization endpoint should redirect to the client application
        assertThat(finalResponse.getStatusCode()).isEqualTo(302);
        String finalLocation = finalResponse.getResponseHeaderValue("Location");

        // Verify the final redirect URL
        assertThat(finalLocation).startsWith("http://localhost:5173");
        assertThat(finalLocation).contains("code=");

        // Extract the code
        String code = finalLocation.substring(finalLocation.indexOf("code=") + 5);
        if (code.contains("&")) {
            code = code.substring(0, code.indexOf("&"));
        }

        // Exchange the code for a token
        MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
                .param("grant_type", "authorization_code")
                .param("code", code)
                .param("redirect_uri", "http://localhost:5173")
                .param("client_id", "frontend-client")
                .param("code_verifier", codeVerifier)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andReturn();

        String tokenResponse = tokenResult.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> tokenMap = mapper.readValue(tokenResponse, Map.class);

        assertThat(tokenMap).containsKey("access_token");
        assertThat(tokenMap).containsKey("id_token");
        assertThat(tokenMap.get("token_type")).isEqualTo("Bearer");
    }
}

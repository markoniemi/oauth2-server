package com.example.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthServerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
                .queryParam("code_challenge", "QYPAZ5NU8yvtlQ9erY0JnaFPZRYIvpSJDOELq7i67og")
                .queryParam("code_challenge_method", "S256"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }
}

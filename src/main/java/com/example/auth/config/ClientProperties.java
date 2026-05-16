package com.example.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.oauth2")
public class ClientProperties {
    private List<ClientConfig> clients = new ArrayList<>();

    @Data
    public static class ClientConfig {
        private String clientId;
        private String clientSecret;
        private String clientAuthenticationMethod = "NONE";
        private List<String> authorizationGrantTypes = new ArrayList<>();
        private List<String> redirectUris = new ArrayList<>();
        private List<String> postLogoutRedirectUris = new ArrayList<>();
        private List<String> scopes = new ArrayList<>();
        private boolean requireProofKey = false;
        private long accessTokenTimeToLive = 3600;
        private long refreshTokenTimeToLive = 604800;
    }
}

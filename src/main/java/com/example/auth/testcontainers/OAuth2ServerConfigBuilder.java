package com.example.auth.testcontainers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OAuth2ServerConfigBuilder {
    private List<OAuth2User> users = new ArrayList<>();
    private List<OAuth2Client> clients = new ArrayList<>();
    private String issuerUrl;
    private String contextPath;

    public OAuth2ServerConfigBuilder withUser(String username, String password, String... roles) {
        Set<String> roleSet = new HashSet<>();
        for (String role : roles) {
            roleSet.add(role);
        }
        users.add(new OAuth2User(username, password, roleSet));
        return this;
    }

    public OAuth2ServerConfigBuilder withOAuth2Client(OAuth2Client client) {
        clients.add(client);
        return this;
    }

    public OAuth2ServerConfigBuilder withIssuerUrl(String issuerUrl) {
        this.issuerUrl = issuerUrl;
        return this;
    }

    public OAuth2ServerConfigBuilder withContextPath(String contextPath) {
        this.contextPath = contextPath;
        return this;
    }

    public OAuth2ServerConfig build() {
        return new OAuth2ServerConfig(users, clients, issuerUrl, contextPath);
    }

    // Internal method for testing duplicate validation
    public void addUser(OAuth2User user) {
        users.add(user);
    }
}

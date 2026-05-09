package com.example.auth.testcontainers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServerConfigBuilder {
    private List<User> users = new ArrayList<>();
    private List<Client> clients = new ArrayList<>();
    private String issuerUrl;
    private String contextPath;

    public ServerConfigBuilder withUser(String username, String password, String... roles) {
        Set<String> roleSet = new HashSet<>();
        for (String role : roles) {
            roleSet.add(role);
        }
        users.add(new User(username, password, roleSet));
        return this;
    }

    public ServerConfigBuilder withOAuth2Client(Client client) {
        clients.add(client);
        return this;
    }

    public ServerConfigBuilder withIssuerUrl(String issuerUrl) {
        this.issuerUrl = issuerUrl;
        return this;
    }

    public ServerConfigBuilder withContextPath(String contextPath) {
        this.contextPath = contextPath;
        return this;
    }

    public ServerConfig build() {
        return new ServerConfig(users, clients, issuerUrl, contextPath);
    }

    // Internal method for testing duplicate validation
    public void addUser(User user) {
        users.add(user);
    }
}

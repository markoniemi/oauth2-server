package com.example.auth.testcontainers;

import lombok.Builder;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ServerConfig {
    private final List<User> users;
    private final List<Client> clients;
    private final String issuerUrl;
    private final String contextPath;

    public ServerConfig(List<User> users, List<Client> clients, String issuerUrl, String contextPath) {
        this.users = users == null ? new ArrayList<>() : List.copyOf(users);
        this.clients = clients == null ? new ArrayList<>() : List.copyOf(clients);
        this.issuerUrl = issuerUrl;
        this.contextPath = contextPath;
    }
}

package com.example.auth.testcontainers;

import lombok.Builder;
import lombok.Data;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        validateNoDuplicateUsernames(this.users);
        validateNoDuplicateClientIds(this.clients);
    }

    private static void validateNoDuplicateUsernames(List<User> users) {
        Set<String> seen = new HashSet<>();
        for (User user : users) {
            if (!seen.add(user.getUsername())) {
                throw new IllegalArgumentException("Username '" + user.getUsername() + "' is duplicated");
            }
        }
    }

    private static void validateNoDuplicateClientIds(List<Client> clients) {
        Set<String> seen = new HashSet<>();
        for (Client client : clients) {
            if (!seen.add(client.getClientId())) {
                throw new IllegalArgumentException("Client ID '" + client.getClientId() + "' is duplicated");
            }
        }
    }
}

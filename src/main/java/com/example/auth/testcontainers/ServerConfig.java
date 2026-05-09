package com.example.auth.testcontainers;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

public record ServerConfig(
    List<User> users,
    List<Client> clients,
    String issuerUrl,
    String contextPath
) {
    public ServerConfig {
        users = List.copyOf(users);
        clients = List.copyOf(clients);

        validateNoDuplicateUsernames(users);
        validateNoDuplicateClientIds(clients);
    }

    private static void validateNoDuplicateUsernames(List<User> users) {
        Set<String> seen = new HashSet<>();
        for (User user : users) {
            if (!seen.add(user.username())) {
                throw new IllegalArgumentException("Username '" + user.username() + "' is duplicated");
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

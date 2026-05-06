package com.example.auth.testcontainers;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

public record OAuth2ServerConfig(
    List<OAuth2User> users,
    List<OAuth2Client> clients,
    String issuerUrl,
    String contextPath
) {
    public OAuth2ServerConfig {
        users = List.copyOf(users);
        clients = List.copyOf(clients);

        validateNoDuplicateUsernames(users);
        validateNoDuplicateClientIds(clients);
    }

    private static void validateNoDuplicateUsernames(List<OAuth2User> users) {
        Set<String> seen = new HashSet<>();
        for (OAuth2User user : users) {
            if (!seen.add(user.username())) {
                throw new IllegalArgumentException("Username '" + user.username() + "' is duplicated");
            }
        }
    }

    private static void validateNoDuplicateClientIds(List<OAuth2Client> clients) {
        Set<String> seen = new HashSet<>();
        for (OAuth2Client client : clients) {
            if (!seen.add(client.getClientId())) {
                throw new IllegalArgumentException("Client ID '" + client.getClientId() + "' is duplicated");
            }
        }
    }
}

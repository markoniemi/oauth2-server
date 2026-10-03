package com.example.auth.testcontainers;

import static java.util.Arrays.asList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;

public class OAuth2Container extends GenericContainer<OAuth2Container> {

    private static final int AUTH_SERVER_PORT = 9000;
    private static final String IMAGE_NAME = "ghcr.io/markoniemi/oauth2-server:latest";

    private final List<User> users = new ArrayList<>();
    private final List<Client> clients = new ArrayList<>();
    private String issuerUrl;

    public OAuth2Container() {
        this(DockerImageName.parse(IMAGE_NAME));
    }

    /** Uses the given image, e.g. to pin a released tag instead of {@code latest}. */
    public OAuth2Container(DockerImageName image) {
        super(image);
        withExposedPorts(AUTH_SERVER_PORT);
        waitingFor(Wait.forHttp("/actuator/health")
            .forStatusCode(200)
            .withStartupTimeout(Duration.ofMinutes(2)));
    }

    public OAuth2Container withUser(String username, String password, String... roles) {
        users.add(new User(username, password, new HashSet<>(asList(roles))));
        return this;
    }

    public OAuth2Container withOAuth2Client(Client client) {
        clients.add(client);
        return this;
    }

    public OAuth2Container withIssuerUrl(String issuerUrl) {
        this.issuerUrl = issuerUrl;
        return this;
    }

    public OAuth2Container withConfigFile(String configResourcePath) {
        withCopyFileToContainer(
            MountableFile.forClasspathResource(configResourcePath),
            "/config/application.yaml");
        return this;
    }

    public String getAuthServerUrl() {
        return "http://localhost:" + getMappedPort(AUTH_SERVER_PORT);
    }

    public String getIssuerUrl() {
        if (issuerUrl != null) {
            return issuerUrl;
        }
        return getAuthServerUrl();
    }

    public List<User> getUsers() {
        return users;
    }

    public List<Client> getClients() {
        return clients;
    }

    @Override
    protected void configure() {
        String yaml = generateConfigYaml();
        if (yaml != null) {
            withCopyToContainer(Transferable.of(yaml), "/config/application.yaml");
        }
    }

    /**
     * Builds the Spring configuration mounted into the container, or {@code null} when there is
     * nothing to configure.
     */
    String generateConfigYaml() {
        if (users.isEmpty() && clients.isEmpty()) {
            return null;
        }

        Map<String, Object> root = new LinkedHashMap<>();

        if (!users.isEmpty()) {
            List<Map<String, Object>> usersList = new ArrayList<>();
            for (User user : users) {
                Map<String, Object> userMap = new LinkedHashMap<>();
                userMap.put("username", user.getUsername());
                userMap.put("password", user.getPassword());
                userMap.put("roles", new ArrayList<>(user.getRoles()));
                usersList.add(userMap);
            }
            Map<String, Object> securityMap = new LinkedHashMap<>();
            securityMap.put("users", usersList);
            Map<String, Object> appMap = new LinkedHashMap<>();
            appMap.put("security", securityMap);
            root.put("app", appMap);
        }

        if (!clients.isEmpty()) {
            Map<String, Object> clientsMap = new LinkedHashMap<>();
            for (Client client : clients) {
                clientsMap.put(client.getClientId(), toClientProperties(client));
            }

            Map<String, Object> authserverMap = new LinkedHashMap<>();
            authserverMap.put("client", clientsMap);
            Map<String, Object> oauth2Map = new LinkedHashMap<>();
            oauth2Map.put("authorizationserver", authserverMap);
            Map<String, Object> secMap = new LinkedHashMap<>();
            secMap.put("oauth2", oauth2Map);
            Map<String, Object> springMap = new LinkedHashMap<>();
            springMap.put("security", secMap);
            root.put("spring", springMap);
        }

        try {
            return new ObjectMapper(new YAMLFactory()).writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to generate container configuration", e);
        }
    }

    /** Maps a client to Spring Boot's {@code spring.security.oauth2.authorizationserver.client.<id>} properties. */
    private static Map<String, Object> toClientProperties(Client client) {
        boolean publicClient = client.isPublicClient();

        List<String> grantTypes = new ArrayList<>(client.getGrantTypes());
        if (publicClient) {
            // The authorization server never issues refresh tokens to public clients
            grantTypes.remove("refresh_token");
        }

        Map<String, Object> registration = new LinkedHashMap<>();
        registration.put("client-id", client.getClientId());
        if (!publicClient) {
            registration.put("client-secret", client.getClientSecret());
        }
        registration.put("client-authentication-methods",
            List.of(publicClient ? "none" : client.getTokenEndpointAuthMethod()));
        registration.put("authorization-grant-types", grantTypes);
        registration.put("redirect-uris", new ArrayList<>(client.getRedirectUris()));
        if (!client.getPostLogoutRedirectUris().isEmpty()) {
            registration.put("post-logout-redirect-uris", new ArrayList<>(client.getPostLogoutRedirectUris()));
        }
        registration.put("scopes", new ArrayList<>(client.getScopes()));

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("registration", registration);
        if (client.isRequireProofKey() || publicClient) {
            properties.put("require-proof-key", true);
        }

        Map<String, Object> token = new LinkedHashMap<>();
        if (client.getAccessTokenTimeToLive() != null) {
            token.put("access-token-time-to-live", client.getAccessTokenTimeToLive().toString());
        }
        if (client.getRefreshTokenTimeToLive() != null) {
            token.put("refresh-token-time-to-live", client.getRefreshTokenTimeToLive().toString());
        }
        if (!token.isEmpty()) {
            properties.put("token", token);
        }
        return properties;
    }
}

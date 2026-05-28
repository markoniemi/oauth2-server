package com.example.auth.testcontainers;

import static java.util.Arrays.asList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class Container extends GenericContainer<Container> {

    private static final int AUTH_SERVER_PORT = 9000;
    private static final String IMAGE_NAME = "ghcr.io/markoniemi/oauth2-server:latest";

    private final List<User> users = new ArrayList<>();
    private final List<Client> clients = new ArrayList<>();
    private String issuerUrl;

    public Container() {
        super(DockerImageName.parse(IMAGE_NAME));
        withExposedPorts(AUTH_SERVER_PORT);
        waitingFor(Wait.forHttp("/actuator/health")
            .forStatusCode(200)
            .withStartupTimeout(Duration.ofMinutes(2)));
    }

    public Container withUser(String username, String password, String... roles) {
        users.add(new User(username, password, new HashSet<>(asList(roles))));
        return this;
    }

    public Container withOAuth2Client(Client client) {
        clients.add(client);
        return this;
    }

    public Container withIssuerUrl(String issuerUrl) {
        this.issuerUrl = issuerUrl;
        return this;
    }

    public Container withConfigFile(String filePath) {
        try {
            String resolvedPath = filePath;
            if (filePath.startsWith("classpath:")) {
                resolvedPath = filePath.substring("classpath:".length());
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                var resource = classLoader.getResource(resolvedPath);
                if (resource == null) {
                    throw new IllegalArgumentException("Classpath resource not found: " + resolvedPath);
                }
                resolvedPath = resource.getPath();
            }

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            @SuppressWarnings("unchecked")
            Map<String, Object> yaml = mapper.readValue(new File(resolvedPath), Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> app = (Map<String, Object>) yaml.get("app");
            if (app != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> security = (Map<String, Object>) app.get("security");
                if (security != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> usersList = (List<Map<String, Object>>) security.get("users");
                    if (usersList != null) {
                        for (Map<String, Object> u : usersList) {
                            String username = (String) u.get("username");
                            String password = (String) u.get("password");
                            @SuppressWarnings("unchecked")
                            List<String> roles = (List<String>) u.get("roles");
                            if (username != null && password != null && roles != null && !roles.isEmpty()) {
                                users.add(new User(username, password, new HashSet<>(roles)));
                            }
                        }
                    }
                }
            }
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config file: " + filePath, e);
        }
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

    @Override
    protected void configure() {
        try {
            generateAndMountConfigYaml();
        } catch (IOException e) {
            throw new RuntimeException("Failed to configure container", e);
        }
    }

    private void generateAndMountConfigYaml() throws IOException {
        if (users.isEmpty() && clients.isEmpty()) {
            return;
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
                Map<String, Object> registration = new LinkedHashMap<>();
                registration.put("client-id", client.getClientId());
                registration.put("client-secret", client.getClientSecret());
                registration.put("client-authentication-methods", List.of("client_secret_basic"));
                registration.put("authorization-grant-types", new ArrayList<>(client.getGrantTypes()));
                registration.put("redirect-uris", new ArrayList<>(client.getRedirectUris()));
                registration.put("scopes", new ArrayList<>(client.getScopes()));

                Map<String, Object> clientEntry = new LinkedHashMap<>();
                clientEntry.put("registration", registration);
                clientsMap.put(client.getClientId(), clientEntry);
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

        File tempDir = Files.createTempDirectory("oauth2-config-").toFile();
        File configFile = new File(tempDir, "application.yaml");
        new ObjectMapper(new YAMLFactory()).writeValue(configFile, root);

        withCopyFileToContainer(
            MountableFile.forHostPath(configFile.getAbsolutePath()),
            "/config/application.yaml");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.delete(configFile.toPath());
                Files.delete(tempDir.toPath());
            } catch (IOException e) {
                // Ignore cleanup errors
            }
        }));
    }
}

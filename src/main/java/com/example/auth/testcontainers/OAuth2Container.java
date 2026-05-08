package com.example.auth.testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.example.auth.testcontainers.config.OAuth2ContainerRegisteredClientConfig;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OAuth2Container extends GenericContainer<OAuth2Container> {

    private static final int AUTH_SERVER_PORT = 9000;
    private static final String IMAGE_NAME = "ghcr.io/markoniemi/oauth2-server:latest";

    private List<OAuth2User> users = new ArrayList<>();
    private List<OAuth2Client> clients = new ArrayList<>();
    private String issuerUrl;
    private String contextPath = "/";

    public OAuth2Container() {
        super(DockerImageName.parse(IMAGE_NAME));
        withExposedPorts(AUTH_SERVER_PORT);
        withEnv("SPRING_PROFILES_ACTIVE", "testcontainers");
    }

    public OAuth2Container withUser(String username, String password, String... roles) {
        Set<String> roleSet = new HashSet<>();
        for (String role : roles) {
            roleSet.add(role);
        }
        users.add(new OAuth2User(username, password, roleSet));
        return this;
    }

    public OAuth2Container withOAuth2Client(OAuth2Client client) {
        clients.add(client);
        return this;
    }

    public OAuth2Container withIssuerUrl(String issuerUrl) {
        this.issuerUrl = issuerUrl;
        return this;
    }

    public OAuth2Container withContextPath(String contextPath) {
        this.contextPath = contextPath;
        return this;
    }

    public OAuth2Container withConfigFile(String filePath) {
        try {
            String resolvedPath = filePath;

            // Handle classpath resources
            if (filePath.startsWith("classpath:")) {
                resolvedPath = filePath.substring("classpath:".length());
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                var resource = classLoader.getResource(resolvedPath);
                if (resource == null) {
                    throw new IllegalArgumentException("Classpath resource not found: " + resolvedPath);
                }
                resolvedPath = resource.getPath();
            }

            // Load YAML and deserialize
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            Map<String, Object> yaml = mapper.readValue(new File(resolvedPath), Map.class);

            // Extract app.security.users
            @SuppressWarnings("unchecked")
            Map<String, Object> app = (Map<String, Object>) yaml.get("app");
            if (app != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> security = (Map<String, Object>) app.get("security");
                if (security != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> usersList = (List<Map<String, Object>>) security.get("users");
                    if (usersList != null) {
                        for (Map<String, Object> userMap : usersList) {
                            String username = (String) userMap.get("username");
                            String password = (String) userMap.get("password");
                            @SuppressWarnings("unchecked")
                            List<String> roles = (List<String>) userMap.get("roles");

                            if (username != null && password != null && roles != null) {
                                users.add(new OAuth2User(username, password, new HashSet<>(roles)));
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
            generateAndMountUsersYaml();
            registerClients();
        } catch (IOException e) {
            throw new RuntimeException("Failed to configure container", e);
        }
    }

    private void generateAndMountUsersYaml() throws IOException {
        if (users.isEmpty()) {
            return;  // No users to configure
        }

        // Build YAML structure
        Map<String, Object> appConfig = new LinkedHashMap<>();
        Map<String, Object> securityConfig = new LinkedHashMap<>();
        List<Map<String, Object>> usersList = new ArrayList<>();

        for (OAuth2User user : users) {
            Map<String, Object> userMap = new LinkedHashMap<>();
            userMap.put("username", user.username());
            userMap.put("password", user.password());
            userMap.put("roles", new ArrayList<>(user.roles()));
            usersList.add(userMap);
        }

        securityConfig.put("users", usersList);
        appConfig.put("security", securityConfig);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("app", appConfig);

        // Write to temp file
        File tempDir = Files.createTempDirectory("oauth2-config-").toFile();
        File configFile = new File(tempDir, "application.yaml");

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.writeValue(configFile, root);

        // Mount the temp file
        withFileSystemBind(configFile.getAbsolutePath(), "/config/application.yaml");

        // Register cleanup hook (will run when JVM exits)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.delete(configFile.toPath());
                Files.delete(tempDir.toPath());
            } catch (IOException e) {
                // Ignore cleanup errors
            }
        }));
    }

    private void registerClients() {
        if (!clients.isEmpty()) {
            OAuth2ContainerRegisteredClientConfig.setClientsToRegister(new ArrayList<>(clients));
        }
    }
}

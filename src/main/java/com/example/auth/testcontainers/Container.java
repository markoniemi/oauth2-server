package com.example.auth.testcontainers;

import static java.util.Arrays.asList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.CollectionUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class Container extends GenericContainer<Container> {

    private static final int AUTH_SERVER_PORT = 9000;
    private static final String IMAGE_NAME = "ghcr.io/markoniemi/oauth2-server:latest";

    private final List<User> users = new ArrayList<>();
    private final List<Client> clients = new ArrayList<>();
    private String issuerUrl;

    public Container() {
        super(DockerImageName.parse(IMAGE_NAME));
        withExposedPorts(AUTH_SERVER_PORT);
        withEnv("SPRING_PROFILES_ACTIVE", "testcontainers");
        waitingFor(Wait.forHttp("/.well-known/openid-configuration").forStatusCode(200));
    }

    public Container withUser(String username, String password, String... roles) {
        users.add(new User(username, password, new HashSet<>(asList( roles))));
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
            @SuppressWarnings("unchecked")
            Map<String, Object> yaml = mapper.readValue(new File(resolvedPath), Map.class);

            // Extract app.security and map to SecurityConfig
            @SuppressWarnings("unchecked")
            Map<String, Object> app = (Map<String, Object>) yaml.get("app");
            if (app != null) {
                Object security = app.get("security");
                if (security != null) {
                    SecurityConfig config = mapper.convertValue(security, SecurityConfig.class);
                    for (UserConfig userConfig : config.getUsers()) {
                        if (userConfig.getUsername() != null && userConfig.getPassword() != null && !userConfig.getRoles().isEmpty()) {
                            users.add(userConfig.toUser());
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

        for (User user : users) {
            Map<String, Object> userMap = new LinkedHashMap<>();
            userMap.put("username", user.getUsername());
            userMap.put("password", user.getPassword());
            userMap.put("roles", new ArrayList<>(user.getRoles()));
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
        if (!CollectionUtils.isEmpty(clients)) {
            ClientConfig.setClients(new ArrayList<>(clients));
        }
    }
}

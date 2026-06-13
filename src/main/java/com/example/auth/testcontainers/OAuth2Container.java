package com.example.auth.testcontainers;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class OAuth2Container extends GenericContainer<OAuth2Container> {

    private static final int AUTH_SERVER_PORT = 9000;
    private static final String IMAGE_NAME = "ghcr.io/markoniemi/oauth2-server:latest";

    private final List<User> users = new ArrayList<>();
    private final List<Client> clients = new ArrayList<>();
    private String issuerUrl;

    public OAuth2Container() {
        super(DockerImageName.parse(IMAGE_NAME));
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

        String yamlContent = generateYamlContent();

        File tempDir = Files.createTempDirectory("oauth2-config-").toFile();
        File configFile = new File(tempDir, "application.yaml");
        Files.writeString(configFile.toPath(), yamlContent);

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

    private String generateYamlContent() {
        StringBuilder yaml = new StringBuilder();

        // Users section
        if (!users.isEmpty()) {
            yaml.append("app:\n");
            yaml.append("  security:\n");
            yaml.append("    users:\n");
            for (User user : users) {
                yaml.append("      - username: ").append(user.getUsername()).append("\n");
                yaml.append("        password: ").append(user.getPassword()).append("\n");
                yaml.append("        roles:\n");
                for (String role : user.getRoles()) {
                    yaml.append("          - ").append(role).append("\n");
                }
            }
        }

        // Clients section
        if (!clients.isEmpty()) {
            yaml.append("spring:\n");
            yaml.append("  security:\n");
            yaml.append("    oauth2:\n");
            yaml.append("      authorizationserver:\n");
            yaml.append("        client:\n");

            for (Client client : clients) {
                yaml.append("          ").append(client.getClientId()).append(":\n");
                yaml.append("            registration:\n");
                yaml.append("              client-id: ").append(client.getClientId()).append("\n");
                yaml.append("              client-secret: ").append(client.getClientSecret()).append("\n");
                yaml.append("              client-authentication-methods:\n");
                yaml.append("                - client_secret_basic\n");

                yaml.append("              authorization-grant-types:\n");
                for (String grantType : client.getGrantTypes()) {
                    yaml.append("                - ").append(grantType).append("\n");
                }

                yaml.append("              redirect-uris:\n");
                for (String uri : client.getRedirectUris()) {
                    yaml.append("                - ").append(uri).append("\n");
                }

                yaml.append("              scopes:\n");
                for (String scope : client.getScopes()) {
                    yaml.append("                - ").append(scope).append("\n");
                }
            }
        }

        return yaml.toString();
    }
}

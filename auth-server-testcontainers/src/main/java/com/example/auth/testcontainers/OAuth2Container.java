package com.example.auth.testcontainers;

import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Runs the OAuth2 authorization server in Docker. Users and clients come from the server's bundled
 * defaults, overridden by the Spring configuration given to {@link #withConfigFile(String)}.
 */
public class OAuth2Container extends GenericContainer<OAuth2Container> {

  private static final int AUTH_SERVER_PORT = 9000;
  private static final String IMAGE_NAME = "ghcr.io/markoniemi/oauth2-server:latest";

  private String issuerUrl;
  private String contextPath = "";

  public OAuth2Container() {
    this(DockerImageName.parse(IMAGE_NAME));
  }

  /** Uses the given image, e.g. to pin a released tag instead of {@code latest}. */
  public OAuth2Container(DockerImageName image) {
    super(image);
    withExposedPorts(AUTH_SERVER_PORT);
    waitForHealth();
  }

  private void waitForHealth() {
    waitingFor(Wait.forHttp(contextPath + "/actuator/health")
        .forStatusCode(200)
        .withStartupTimeout(Duration.ofMinutes(2)));
  }

  /**
   * Fixes the issuer ({@code iss} claim and discovery). Without it the server derives the
   * issuer from each request, which matches {@link #getAuthServerUrl()}.
   */
  public OAuth2Container withIssuerUrl(String issuerUrl) {
    this.issuerUrl = issuerUrl;
    return withEnv("SPRING_SECURITY_OAUTH2_AUTHORIZATIONSERVER_ISSUER", issuerUrl);
  }

  /** Serves the authorization server under the given servlet context path, e.g. {@code /auth}. */
  public OAuth2Container withContextPath(String contextPath) {
    this.contextPath = contextPath;
    waitForHealth();
    return withEnv("SERVER_SERVLET_CONTEXT_PATH", contextPath);
  }

  /**
   * Mounts a Spring configuration file from the classpath as the server's
   * {@code config/application.yaml}. Its values override the server's bundled defaults: maps such
   * as clients merge by key, while single values and lists are replaced.
   */
  public OAuth2Container withConfigFile(String configResourcePath) {
    withCopyFileToContainer(
      MountableFile.forClasspathResource(configResourcePath),
      "/config/application.yaml");
    return this;
  }

  public String getAuthServerUrl() {
    return "http://localhost:" + getMappedPort(AUTH_SERVER_PORT) + contextPath;
  }

  public String getIssuerUrl() {
    if (issuerUrl != null) {
      return issuerUrl;
    }
    return getAuthServerUrl();
  }
}

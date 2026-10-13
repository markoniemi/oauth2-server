package com.example.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/** Browser apps call the token endpoint cross-origin, so the preflight request must be answered. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "app.cors.allowed-origins=http://localhost:5173")
public class CorsIT {

  private final HttpClient client = HttpClient.newHttpClient();

  @LocalServerPort
  private int port;

  @Test
  public void preflightFromAllowedOriginIsAccepted() throws Exception {
    HttpResponse<String> response = preflight("http://localhost:5173");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Access-Control-Allow-Origin"))
        .hasValue("http://localhost:5173");
  }

  @Test
  public void preflightFromUnknownOriginIsRejected() throws Exception {
    HttpResponse<String> response = preflight("http://evil.example");

    assertThat(response.statusCode()).isEqualTo(403);
    assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
  }

  private HttpResponse<String> preflight(String origin) throws Exception {
    HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/oauth2/token"))
        .method("OPTIONS", BodyPublishers.noBody())
        .header("Origin", origin)
        .header("Access-Control-Request-Method", "POST")
        .build();
    return client.send(request, BodyHandlers.ofString());
  }
}

package com.example.auth.config;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Browser origins allowed to call the authorization server (token, userinfo, logout endpoints). */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

  public CorsProperties {
    allowedOrigins = emptyIfNull(allowedOrigins);
  }
}

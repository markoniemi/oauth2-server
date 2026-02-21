package com.example.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
    private List<User> users = new ArrayList<>();

    @Data
    public static class User {
        private String username;
        private String password;
        private List<String> roles = new ArrayList<>();
    }
}

package com.example.auth.testcontainers;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class SecurityConfig {
    private List<UserConfig> users = new ArrayList<>();
}

package com.example.auth.testcontainers;

import lombok.Data;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Data
public class UserConfig {
    private String username;
    private String password;
    private List<String> roles = new ArrayList<>();

    public User toUser() {
        return new User(username, password, new HashSet<>(roles));
    }
}

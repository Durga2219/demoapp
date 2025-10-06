package com.example.demo.dto;

import com.example.demo.enums.Role;

public class AuthResponse {
    private String token;
    private final String type = "Bearer";  // Fixed as final
    private Long id;
    private String username;
    private String email;
    private String name;
    private Role role;

    public AuthResponse(String token, Long id, String username, String email, String name, Role role) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.name = name;
        this.role = role;
    }

    // Getters only for final field
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getType() { return type; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}

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
    
    // Vehicle details for drivers
    private String vehicleModel;
    private String vehiclePlate;
    private Integer vehicleCapacity;
    private String profilePicture;
    private String redirectUrl;

    public AuthResponse(String token, Long id, String username, String email, String name, Role role) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.name = name;
        this.role = role;
    }
    
    // Constructor with vehicle details
    public AuthResponse(String token, Long id, String username, String email, String name, Role role,
                       String vehicleModel, String vehiclePlate, Integer vehicleCapacity, String profilePicture) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.name = name;
        this.role = role;
        this.vehicleModel = vehicleModel;
        this.vehiclePlate = vehiclePlate;
        this.vehicleCapacity = vehicleCapacity;
        this.profilePicture = profilePicture;
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
    
    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }
    
    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }
    
    public Integer getVehicleCapacity() { return vehicleCapacity; }
    public void setVehicleCapacity(Integer vehicleCapacity) { this.vehicleCapacity = vehicleCapacity; }
    
    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }
    
    public String getRedirectUrl() { return redirectUrl; }
    public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }
}

package com.example.demo.dto;

import jakarta.validation.constraints.*;

public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private String phoneNumber;

    @NotBlank(message = "Role is required")
    private String role;  // Should be DRIVER, PASSENGER, or BOTH

    // Optional vehicle details
    private String vehicleModel;
    private String vehiclePlate;
    @Min(value = 1, message = "Vehicle capacity must be at least 1")
    @Max(value = 8, message = "Vehicle capacity cannot exceed 8")
    private Integer vehicleCapacity;

    public RegisterRequest() {}

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }

    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }

    public Integer getVehicleCapacity() { return vehicleCapacity; }
    public void setVehicleCapacity(Integer vehicleCapacity) { this.vehicleCapacity = vehicleCapacity; }
}

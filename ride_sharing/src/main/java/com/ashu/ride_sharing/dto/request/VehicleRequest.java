package com.ashu.ride_sharing.dto.request;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.ashu.ride_sharing.models.enums.VehicleType;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class VehicleRequest {
    @NotBlank(message = "Vehicle make is required")
    @Size(min = 2, max = 50, message = "Make must be between 2 and 50 characters")
    private String make;

    @NotBlank(message = "Vehicle model is required")
    @Size(min = 2, max = 50, message = "Model must be between 2 and 50 characters")
    private String model;

    @Size(max = 30, message = "Color cannot exceed 30 characters")
    private String color;

    @NotBlank(message = "License plate is required")
    @Pattern(regexp = "^[A-Z0-9\\-\\s]{3,15}$", message = "Invalid license plate format")
    private String licensePlate;

    @NotNull(message = "Vehicle year is required")
    @Min(value = 1990, message = "Vehicle must be 1990 or newer")
    @Max(value = 2025, message = "Invalid vehicle year")
    private Integer year;

    @NotNull(message = "Vehicle capacity is required")
    @Min(value = 2, message = "Vehicle must have at least 2 seats")
    @Max(value = 8, message = "Vehicle cannot have more than 8 seats")
    private Integer capacity;

    @NotNull(message = "Vehicle type is required")
    private VehicleType type;

    // This field was added to accept multiple image files
    private List<MultipartFile> images;
}
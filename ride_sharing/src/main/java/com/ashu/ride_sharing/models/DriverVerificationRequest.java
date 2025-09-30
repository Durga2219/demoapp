package com.ashu.ride_sharing.models;

import java.time.LocalDate;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DriverVerificationRequest {
    @NotBlank(message = "Driver license number is required")
    @Pattern(regexp = "^[A-Z0-9]{8,20}$", message = "Invalid license format")
    private String driverLicenseNumber;

    @NotNull(message = "License expiry date is required")
    @Future(message = "License must not be expired")
    private LocalDate licenseExpiryDate;

    @NotBlank(message = "License issuing state is required")
    private String licenseIssuingState;
}
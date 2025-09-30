package com.ashu.ride_sharing.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String phoneNumber;
    private String address;
    private String city;
    private String state;
    private String zipCode;
}

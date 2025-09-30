package com.ashu.ride_sharing.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.ashu.ride_sharing.models.enums.UserRole;
import com.ashu.ride_sharing.models.enums.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {
    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phoneNumber;
    private UserRole role;
    private UserStatus status;
    private String profilePictureUrl;
    private BigDecimal rating;
    private Integer totalRatings;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    
    // Driver specific fields
    private Boolean driverVerified;
    private String driverLicenseNumber;
    private java.time.LocalDate licenseExpiryDate;
    private String licenseIssuingState;
}

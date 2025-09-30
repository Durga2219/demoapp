package com.ashu.ride_sharing.dto.response;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DriverStatusResponse {
    private Boolean driverVerified;
    private String driverLicenseNumber;
    private LocalDate licenseExpiryDate;
    private String licenseIssuingState;
    private List<VehicleResponse> vehicles;
    private String message;
}
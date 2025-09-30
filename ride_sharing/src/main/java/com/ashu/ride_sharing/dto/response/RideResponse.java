package com.ashu.ride_sharing.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.ashu.ride_sharing.models.enums.RideStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RideResponse {
    private UUID rideId;
    private String driverName;
    private VehicleResponse vehicle;
    private String sourceCity;
    private String sourceAddress;
    private String destinationCity;
    private String destinationAddress;
    private LocalDateTime departureDateTime;
    private Integer availableSeats;
    private Integer totalSeats;
    private BigDecimal baseFare;
    private String description;
    private RideStatus status;
    private String routeInfo;
}
package com.ashu.ride_sharing.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class BookingRequest {
    @NotNull(message = "Ride ID is required")
    private UUID rideId;

    @NotNull(message = "Number of seats is required")
    @Min(value = 1, message = "Must request at least 1 seat")
    @Max(value = 8, message = "Cannot request more than 8 seats")
    private Integer seatsRequested;

    @Size(max = 255, message = "Pickup location cannot exceed 255 characters")
    private String pickupLocation;

    @Size(max = 255, message = "Dropoff location cannot exceed 255 characters")
    private String dropoffLocation;
}
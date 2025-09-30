package com.ashu.ride_sharing.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateRideRequest {
    @NotNull(message = "Vehicle ID is required")
    private UUID vehicleId;

    @NotBlank(message = "Source city is required")
    @Size(min = 2, max = 100, message = "Source city must be between 2 and 100 characters")
    private String sourceCity;

    @NotBlank(message = "Source address is required")
    @Size(max = 255, message = "Source address cannot exceed 255 characters")
    private String sourceAddress;

    @NotBlank(message = "Destination city is required")
    @Size(min = 2, max = 100, message = "Destination city must be between 2 and 100 characters")
    private String destinationCity;

    @NotBlank(message = "Destination address is required")
    @Size(max = 255, message = "Destination address cannot exceed 255 characters")
    private String destinationAddress;

    @NotNull(message = "Departure date and time is required")
    @Future(message = "Departure time must be in the future")
    private LocalDateTime departureDateTime;

    @NotNull(message = "Available seats is required")
    @Min(value = 1, message = "Must have at least 1 available seat")
    @Max(value = 8, message = "Cannot exceed 8 seats")
    private Integer availableSeats;

    @NotNull(message = "Base fare is required")
    @DecimalMin(value = "0.01", message = "Base fare must be greater than 0")
    @DecimalMax(value = "9999.99", message = "Base fare cannot exceed 9999.99")
    private BigDecimal baseFare;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
}
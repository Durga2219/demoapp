package com.ashu.ride_sharing.models.enums;

public enum RideStatus {
    ACTIVE,           // Available for booking
    FULL,            // No seats available
    CANCELLED,       // Cancelled by driver
    COMPLETED,       // Trip completed
    IN_PROGRESS,     // Currently ongoing
    EXPIRED          // Past departure time
}
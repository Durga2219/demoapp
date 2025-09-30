package com.ashu.ride_sharing.models.enums;

public enum BookingStatus {
    PENDING,         // Waiting for confirmation
    CONFIRMED,       // Confirmed by driver/system
    CANCELLED,       // Cancelled by passenger/driver
    COMPLETED,       // Trip completed
    NO_SHOW,         // Passenger didn't show up
    REFUNDED         // Booking cancelled and refunded
}
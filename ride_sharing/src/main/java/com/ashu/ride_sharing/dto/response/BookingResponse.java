package com.ashu.ride_sharing.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.ashu.ride_sharing.models.enums.BookingStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingResponse {
    private UUID bookingId;
    private String bookingReference;
    private RideResponse ride;
    private String passengerName;
    private String passengerEmail;
    private Integer seatsBooked;
    private BigDecimal totalFare;
    private BookingStatus status;
    private String pickupLocation;
    private String dropoffLocation;
    private LocalDateTime bookedAt;
    private String message;
}
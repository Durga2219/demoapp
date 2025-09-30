package com.ashu.ride_sharing.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.ashu.ride_sharing.models.enums.BookingStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@Table(name = "bookings")
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private User passenger;

    @Column(nullable = false)
    private Integer seatsBooked;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalFare;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;


    private String pickupLocation;
    private String dropoffLocation;

    private Double pickupLat;
    private Double pickupLag;
    private Double dropoffLat;
    private Double dropoffLag;

    private LocalDateTime pickupTime;
    private LocalDateTime dropoffTime;


    @Column(columnDefinition = "TEXT")
    private String specialRequest;

    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime bookedAt;

    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime completedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private String cancellationReason;


    private String paymentId;
    private String paymentStatus;


    private Integer passengerRating;
    private Integer driverRating;

    @Column(columnDefinition ="TEXT")
    private String passengerReview;

    @Column(columnDefinition = "TEXT")
    private String driverReview;

    public void confirm(){
        this.status = BookingStatus.CONFIRMED;
        this.confirmedAt=LocalDateTime.now();
    }

    public void cancel(String reason){
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt =LocalDateTime.now();
        this.cancellationReason=reason;
    }

    public void complete(){
        this.status= BookingStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public boolean canBeCancelled(){
        return (status == BookingStatus.PENDING) || (status == BookingStatus.CONFIRMED);
    }

    public String getBookingReference(){
        return bookingId.toString().substring(0,8).toUpperCase();
    }


}

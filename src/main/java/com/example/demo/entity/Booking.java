package com.example.demo.entity;

import jakarta.persistence.*;
import com.example.demo.enums.BookingStatus;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @ManyToOne
    @JoinColumn(name = "passenger_id", nullable = false)
    private User passenger;

    private Integer seatsBooked;
    private String pickupLocation;
    private String dropLocation;
    
    // ==================== ENHANCED FARE FIELDS ====================
    private Double distance; // Distance in kilometers for this passenger
    private Double fare; // Original fare calculation
    private Double calculatedFare; // Auto-calculated based on distance and pricePerKm
    // ==================== END ENHANCED FIELDS ====================

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BookingStatus status;

    private LocalDateTime bookedAt = LocalDateTime.now();
    private LocalDateTime cancelledAt;

    // ==================== LIFECYCLE CALLBACKS ====================
    @PrePersist
    public void prePersist() {
        if (bookedAt == null) {
            bookedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = BookingStatus.PENDING;
        }
    }

    // ==================== BUSINESS LOGIC ====================
    /**
     * Calculate fare based on distance and ride's price per km
     * Formula: (Base Fare + Distance × Price Per KM) × (Seats Booked / Total Seats)
     */
    public void calculateFare(double baseFare, double pricePerKm, int totalSeats) {
        if (distance != null && distance > 0) {
            double totalFare = baseFare + (distance * pricePerKm);
            this.calculatedFare = (totalFare / totalSeats) * seatsBooked;
        } else {
            // Fallback: proportional fare based on ride's total fare
            if (ride != null && ride.getFare() != null && ride.getFare() > 0) {
                this.calculatedFare = (ride.getFare() / totalSeats) * seatsBooked;
            } else {
                this.calculatedFare = 0.0;
            }
        }
        this.fare = this.calculatedFare; // Keep both for backward compatibility
    }

    // ==================== GETTERS & SETTERS ====================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Ride getRide() { return ride; }
    public void setRide(Ride ride) { this.ride = ride; }

    public User getPassenger() { return passenger; }
    public void setPassenger(User passenger) { this.passenger = passenger; }

    public Integer getSeatsBooked() { return seatsBooked; }
    public void setSeatsBooked(Integer seatsBooked) { this.seatsBooked = seatsBooked; }

    public String getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(String pickup) { this.pickupLocation = pickup; }

    public String getDropLocation() { return dropLocation; }
    public void setDropLocation(String drop) { this.dropLocation = drop; }

    public Double getDistance() { return distance; }
    public void setDistance(Double distance) { this.distance = distance; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public LocalDateTime getBookedAt() { return bookedAt; }
    public void setBookedAt(LocalDateTime bookedAt) { this.bookedAt = bookedAt; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public Double getFare() { return fare != null ? fare : 0.0; }
    public void setFare(Double fare) { this.fare = fare; }

    public Double getCalculatedFare() { return calculatedFare != null ? calculatedFare : 0.0; }
    public void setCalculatedFare(Double calculatedFare) { this.calculatedFare = calculatedFare; }
}
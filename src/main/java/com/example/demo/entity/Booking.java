package com.example.demo.entity;

import jakarta.persistence.*;
import com.example.demo.enums.BookingStatus;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    private Double fare;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    private LocalDateTime bookedAt = LocalDateTime.now();
    private LocalDateTime cancelledAt;

    // Getters & Setters
    public Long getId() { return id; }
    public Ride getRide() { return ride; }
    public User getPassenger() { return passenger; }
    public Integer getSeatsBooked() { return seatsBooked; }
    public String getPickupLocation() { return pickupLocation; }
    public String getDropLocation() { return dropLocation; }
    public Double getFare() { return fare; }
    public BookingStatus getStatus() { return status; }
    public LocalDateTime getBookedAt() { return bookedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }

    public void setRide(Ride ride) { this.ride = ride; }
    public void setPassenger(User passenger) { this.passenger = passenger; }
    public void setSeatsBooked(Integer seatsBooked) { this.seatsBooked = seatsBooked; }
    public void setPickupLocation(String pickup) { this.pickupLocation = pickup; }
    public void setDropLocation(String drop) { this.dropLocation = drop; }
    public void setFare(Double fare) { this.fare = fare; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
}

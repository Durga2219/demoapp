package com.example.demo.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "rides")
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "driver_id", nullable = false)
    @JsonIgnoreProperties({"rides","bookings","password"})
    private User driver;

    private String source;
    private String destination;
    private LocalDate date;
    private LocalTime time;
    private Integer totalSeats;
    private Integer availableSeats;
    private String vehicleModel;
    private String vehiclePlate;
    private String contactNumber;
    private String notes;
    private Double pricePerKm;
    private String status;
    
    @Column(nullable = false, columnDefinition = "DOUBLE DEFAULT 0.0")
    private Double fare = 0.0; // total fare for the ride - initialized with default value
    
    @Column(nullable = true, columnDefinition = "DOUBLE DEFAULT 0.0")
    private Double distanceKm = 0.0; // Distance between source and destination in kilometers

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Booking> bookings;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if(createdAt == null) createdAt = now;
        if(updatedAt == null) updatedAt = now;
        if(pricePerKm == null) pricePerKm = 10.0;
        if(status == null) status = "ACTIVE";
        if(availableSeats == null && totalSeats != null) availableSeats = totalSeats;
        if(fare == null) fare = 0.0;
        if(distanceKm == null) distanceKm = 0.0;
    }

    @PreUpdate
    public void preUpdate() { 
        updatedAt = LocalDateTime.now();
        // Ensure fare is never null on update
        if(fare == null) fare = 0.0;
    }

    // -------------------- Getters & Setters --------------------
    public Long getId() { return id; }
    public User getDriver() { return driver; }
    public String getSource() { return source; }
    public String getDestination() { return destination; }
    public LocalDate getDate() { return date; }
    public LocalTime getTime() { return time; }
    public Integer getTotalSeats() { return totalSeats; }
    public Integer getAvailableSeats() { return availableSeats; }
    public String getVehicleModel() { return vehicleModel; }
    public String getVehiclePlate() { return vehiclePlate; }
    public String getContactNumber() { return contactNumber; }
    public String getNotes() { return notes; }
    public Double getPricePerKm() { return pricePerKm; }
    public String getStatus() { return status; }
    
    // Null-safe getter for fare
    public Double getFare() { 
        return fare != null ? fare : 0.0; 
    }
    
    // Null-safe getter for distance
    public Double getDistanceKm() {
        return distanceKm != null ? distanceKm : 0.0;
    }

    public void setDriver(User driver) { this.driver = driver; }
    public void setSource(String source) { this.source = source; }
    public void setDestination(String destination) { this.destination = destination; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setTime(LocalTime time) { this.time = time; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }
    public void setAvailableSeats(Integer availableSeats) { this.availableSeats = availableSeats; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setPricePerKm(Double pricePerKm) { this.pricePerKm = pricePerKm; }
    public void setStatus(String status) { this.status = status; }
    public void setFare(Double fare) { this.fare = fare != null ? fare : 0.0; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm != null ? distanceKm : 0.0; }
    
    public List<Booking> getBookings() { return bookings; }
    public void setBookings(List<Booking> bookings) { this.bookings = bookings; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
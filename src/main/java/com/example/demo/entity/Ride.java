package com.example.demo.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "rides")
public class Ride {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    private Double pricePerKm;
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("ride")
    private List<Booking> bookings;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if(createdAt == null) createdAt = now;
        if(updatedAt == null) updatedAt = now;
        if(pricePerKm == null) pricePerKm = 10.0;
        if(status == null) status = "ACTIVE";
        if(availableSeats == null && totalSeats != null) availableSeats = totalSeats;
    }

    @PreUpdate
    public void preUpdate() { updatedAt = LocalDateTime.now(); }

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
    public Double getPricePerKm() { return pricePerKm; }
    public String getStatus() { return status; }

    public void setDriver(User driver) { this.driver = driver; }
    public void setSource(String source) { this.source = source; }
    public void setDestination(String destination) { this.destination = destination; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setTime(LocalTime time) { this.time = time; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }
    public void setAvailableSeats(Integer availableSeats) { this.availableSeats = availableSeats; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }
    public void setPricePerKm(Double pricePerKm) { this.pricePerKm = pricePerKm; }
    public void setStatus(String status) { this.status = status; }
}

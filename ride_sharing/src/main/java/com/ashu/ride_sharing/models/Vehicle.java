package com.ashu.ride_sharing.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.ashu.ride_sharing.models.enums.VehicleStatus;
import com.ashu.ride_sharing.models.enums.VehicleType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@Table(name = "vehicles")
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID vehicleId;

    // This mapping is correct. It tells Hibernate to create and use a 
    // column named "driver_id" to link to the User table.
    // The error indicates your database has an extra, unmapped column named "driver".
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @Column(nullable = false)
    private String make;

    @Column(nullable = false)
    private String model;

    private Integer year;

    private String color;

    @Column(unique = true, nullable = false)
    private String licensePlate;

    @Column(nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    private VehicleType type = VehicleType.SEDAN;

    @Enumerated(EnumType.STRING)
    private VehicleStatus status = VehicleStatus.ACTIVE;

    private String insuranceNumber;
    private LocalDate insuranceExpiry;
    private String registrationNumber;
    private LocalDate registrationExpiry;

    @ElementCollection
    @CollectionTable(name = "vehicle_images", joinColumns = @JoinColumn(name = "vehicle_id"))
    @Column(name = "image_url", columnDefinition = "TEXT")
    private List<String> vehicleImages;

    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Ride> rides;

    public Integer getAvailableSeats() {
        return capacity - 1;
    }

    public String getVehicleInfo() {
        return (year != null ? year + " " : "") + make + " " + model;
    }
}
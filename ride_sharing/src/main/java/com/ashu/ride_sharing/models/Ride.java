package com.ashu.ride_sharing.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.ashu.ride_sharing.models.enums.RideStatus;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@Table(name = "rides")
@NoArgsConstructor
@AllArgsConstructor
public class Ride {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID rideId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(nullable = false)
    private String sourceCity;

    @Column(nullable = false)
    private String sourceAddress;

    private Double sourceLat;

    private Double sourceLng;

    @Column(nullable = false)
    private String destinationCity;

    @Column(nullable = false)
    private String destinationAddress;

    
    private Double destinationLat;
    private Double destinationLng;

    @Column(nullable = false)
    private LocalDateTime departureDateTime;

    private LocalDateTime estimatedArrivalDateTime;

    @Column(nullable = false)
    private Integer availableSeats;

    @Column(nullable = false)
    private Integer totalSeats;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFare;

    @Column(precision = 10, scale = 2)
    private BigDecimal pricePerKM = BigDecimal.ZERO;

    private Double totalDistanceKm;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RideStatus status = RideStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String route;

    @Builder.Default
    private Boolean smokingAllowed = false;

    @Builder.Default
    private Boolean petsAllowed = false;

    @Builder.Default
    private Boolean luggageAllowed = true;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "ride", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Booking> bookings;
    

    public boolean hasAvailableSeats(){
        return availableSeats > 0;
    }

    public boolean canBook(int requestedSeats){
        return availableSeats >= requestedSeats && status == RideStatus.ACTIVE;
    }

    public void bookSeats(int seatsToBook){
        if (canBook(seatsToBook)) {
            this.availableSeats -=seatsToBook;
            if (this.availableSeats==0) {
                this.status = RideStatus.FULL;
            }
        }
    }

    public void cancelSeats(int seatsToCancel){
        this.availableSeats = Math.min(totalSeats, availableSeats+seatsToCancel);
        if (this.status == RideStatus.FULL && this.availableSeats>0) {
            this.status = RideStatus.ACTIVE;
        }
    }

    public String getRouteInfo(){
        return sourceCity + " -> " + destinationCity;
    }

    public boolean isExpired(){
        return LocalDateTime.now().isAfter(departureDateTime);
    }
}

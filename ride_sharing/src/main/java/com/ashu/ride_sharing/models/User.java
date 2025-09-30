package com.ashu.ride_sharing.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.ashu.ride_sharing.models.enums.UserRole;
import com.ashu.ride_sharing.models.enums.UserStatus;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@Entity
@Builder
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;
    
    private String phoneNumber;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Column(nullable = false,columnDefinition = "TEXT")
    private String profilePictureUrl;

    @Column(precision = 2, scale = 1)
    private BigDecimal rating = BigDecimal.ZERO; 

    private Integer totalRatings =0;

    // Address
    private String address;
    private String city;
    private String state;
    private String zipCode;


    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;
    
    @Column(nullable = false)
     @Builder.Default
    private Boolean phoneVerified = false;
    
    // Relationships
    // @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private List<Vehicle> vehicles;
    
    // @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private List<Ride> ridesAsDriver;
    
    // @OneToMany(mappedBy = "passenger", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private List<Booking> bookingsAsPassenger;
    
    
    public User(String email, String password, String firstName, String lastName, UserRole role) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.createdAt = LocalDateTime.now();
        this.status = UserStatus.ACTIVE;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public void updateRating(double newRating) {
        BigDecimal currentTotal = this.rating.multiply(new BigDecimal(this.totalRatings));
        BigDecimal newTotal = currentTotal.add(new BigDecimal(newRating));
        this.totalRatings++;
        this.rating = newTotal.divide(new BigDecimal(this.totalRatings), 1, RoundingMode.HALF_UP);
    }




    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'getAuthorities'");
        return List.of(new SimpleGrantedAuthority("ROLE_"+role.name()));
    }

    @Override
    public String getPassword() {
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'getPassword'");
        return password;
    }

    @Override
    public String getUsername() {
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'getUsername'");
        return email;
    }


    @Override
    public boolean isAccountNonExpired() {
        return true; // Add logic if accounts can expire
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Add logic for account locking
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Add logic if credentials can expire
    }

        @Override
    public boolean isEnabled() {
        return emailVerified; // Controlled by email verification
    }



    private String driverLicenseNumber;
    private LocalDate licenseExpiryDate;
    private String licenseIssuingState;
    
    @Builder.Default
    private Boolean driverVerified = false;
    
    public boolean canPostRides() {
        return role == UserRole.DRIVER && driverVerified;
    }
    
    public boolean needsDriverVerification() {
        return role == UserRole.DRIVER && !driverVerified;
    }
}

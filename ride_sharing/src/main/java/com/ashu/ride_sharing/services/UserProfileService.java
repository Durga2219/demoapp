package com.ashu.ride_sharing.services;


import java.math.BigDecimal;
import java.util.List;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ashu.ride_sharing.dto.request.UpdateProfileRequest;
import com.ashu.ride_sharing.dto.response.BookingResponse;
import com.ashu.ride_sharing.dto.response.RideResponse;
import com.ashu.ride_sharing.dto.response.UserDashboardResponse;
import com.ashu.ride_sharing.dto.response.UserProfileResponse;
import com.ashu.ride_sharing.dto.response.VehicleResponse;
import com.ashu.ride_sharing.models.Booking;
import com.ashu.ride_sharing.models.Ride;
import com.ashu.ride_sharing.models.User;
import com.ashu.ride_sharing.models.Vehicle;
import com.ashu.ride_sharing.models.enums.BookingStatus;
import com.ashu.ride_sharing.models.enums.RideStatus;
import com.ashu.ride_sharing.models.enums.UserRole;
import com.ashu.ride_sharing.models.enums.VehicleStatus;
import com.ashu.ride_sharing.repositories.BookingRepository;
import com.ashu.ride_sharing.repositories.RideRepository;
import com.ashu.ride_sharing.repositories.UserRepository;
import com.ashu.ride_sharing.repositories.VehicleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserProfileService {
    
    private final UserRepository userRepository;
    private final RideRepository rideRepository;
    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return mapToUserProfileResponse(user);
    }

    public UserProfileResponse updateProfile(String userEmail, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Update profile fields
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        user.setCity(request.getCity());
        user.setState(request.getState());
        user.setZipCode(request.getZipCode());

        User updatedUser = userRepository.save(user);
        log.info("User profile updated: {}", userEmail);

        return mapToUserProfileResponse(updatedUser);
    }

    @Transactional(readOnly = true)
    public UserDashboardResponse getUserDashboard(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        UserProfileResponse profile = mapToUserProfileResponse(user);
        UserDashboardResponse.DashboardStats stats = calculateDashboardStats(user);
        
        List<RideResponse> recentRides = getRecentRides(user);
        List<BookingResponse> recentBookings = getRecentBookings(user);
        List<VehicleResponse> vehicles = user.getRole() == UserRole.DRIVER ? 
                getUserVehicles(user) : List.of();

        return UserDashboardResponse.builder()
                .profile(profile)
                .stats(stats)
                .recentRides(recentRides)
                .recentBookings(recentBookings)
                .vehicles(vehicles)
                .build();
    }

    private UserDashboardResponse.DashboardStats calculateDashboardStats(User user) {
        UserDashboardResponse.DashboardStats.DashboardStatsBuilder statsBuilder = 
                UserDashboardResponse.DashboardStats.builder();

        if (user.getRole() == UserRole.DRIVER) {
            // Driver stats
            List<Ride> allRides = rideRepository.findByDriver_IdOrderByDepartureDateTimeDesc(user.getId());
            List<Booking> driverBookings = bookingRepository.findByRide_Driver_IdOrderByBookedAtDesc(user.getId());
            
            long completedRides = allRides.stream()
                    .mapToLong(ride -> ride.getStatus() == RideStatus.COMPLETED ? 1 : 0)
                    .sum();
            
            long cancelledBookings = driverBookings.stream()
                    .mapToLong(booking -> booking.getStatus() == BookingStatus.CANCELLED ? 1 : 0)
                    .sum();
            
            BigDecimal totalEarnings = driverBookings.stream()
                    .filter(booking -> booking.getStatus() == BookingStatus.COMPLETED)
                    .map(Booking::getTotalFare)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            statsBuilder
                .totalRides((long) allRides.size())
                .totalBookings((long) driverBookings.size())
                .completedRides(completedRides)
                .cancelledBookings(cancelledBookings)
                .totalEarnings(totalEarnings)
                .totalSpent(BigDecimal.ZERO);
        } else {
            // Passenger stats
            List<Booking> passengerBookings = bookingRepository.findByPassenger_IdOrderByBookedAtDesc(user.getId());
            
            long completedRides = passengerBookings.stream()
                    .mapToLong(booking -> booking.getStatus() == BookingStatus.COMPLETED ? 1 : 0)
                    .sum();
            
            long cancelledBookings = passengerBookings.stream()
                    .mapToLong(booking -> booking.getStatus() == BookingStatus.CANCELLED ? 1 : 0)
                    .sum();
            
            BigDecimal totalSpent = passengerBookings.stream()
                    .filter(booking -> booking.getStatus() == BookingStatus.COMPLETED)
                    .map(Booking::getTotalFare)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            statsBuilder
                .totalRides(0L)
                .totalBookings((long) passengerBookings.size())
                .completedRides(completedRides)
                .cancelledBookings(cancelledBookings)
                .totalEarnings(BigDecimal.ZERO)
                .totalSpent(totalSpent);
        }

        return statsBuilder.build();
    }

    private List<RideResponse> getRecentRides(User user) {
        if (user.getRole() != UserRole.DRIVER) {
            return List.of();
        }

        List<Ride> recentRides = rideRepository.findByDriver_IdOrderByDepartureDateTimeDesc(user.getId())
                .stream()
                .limit(5)
                .toList();

        return recentRides.stream()
                .map(this::mapToRideResponse)
                .toList();
    }

    private List<BookingResponse> getRecentBookings(User user) {
        List<Booking> recentBookings;
        
        if (user.getRole() == UserRole.DRIVER) {
            recentBookings = bookingRepository.findByRide_Driver_IdOrderByBookedAtDesc(user.getId())
                    .stream()
                    .limit(5)
                    .toList();
        } else {
            recentBookings = bookingRepository.findByPassenger_IdOrderByBookedAtDesc(user.getId())
                    .stream()
                    .limit(5)
                    .toList();
        }

        return recentBookings.stream()
                .map(this::mapToBookingResponse)
                .toList();
    }

    private List<VehicleResponse> getUserVehicles(User user) {
        List<Vehicle> vehicles = vehicleRepository.findByDriverIdAndStatus(user.getId(), VehicleStatus.ACTIVE);
        return vehicles.stream()
                .map(this::mapToVehicleResponse)
                .toList();
    }

    private UserProfileResponse mapToUserProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .status(user.getStatus())
                .profilePictureUrl(user.getProfilePictureUrl())
                .rating(user.getRating())
                .totalRatings(user.getTotalRatings())
                .address(user.getAddress())
                .city(user.getCity())
                .state(user.getState())
                .zipCode(user.getZipCode())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .driverVerified(user.getDriverVerified())
                .driverLicenseNumber(user.getDriverLicenseNumber())
                .licenseExpiryDate(user.getLicenseExpiryDate())
                .licenseIssuingState(user.getLicenseIssuingState())
                .build();
    }

    private RideResponse mapToRideResponse(Ride ride) {
        return RideResponse.builder()
                .rideId(ride.getRideId())
                .driverName(ride.getDriver().getFullName())
                .vehicle(mapToVehicleResponse(ride.getVehicle()))
                .sourceCity(ride.getSourceCity())
                .sourceAddress(ride.getSourceAddress())
                .destinationCity(ride.getDestinationCity())
                .destinationAddress(ride.getDestinationAddress())
                .departureDateTime(ride.getDepartureDateTime())
                .availableSeats(ride.getAvailableSeats())
                .totalSeats(ride.getTotalSeats())
                .baseFare(ride.getBaseFare())
                .description(ride.getDescription())
                .status(ride.getStatus())
                .routeInfo(ride.getRouteInfo())
                .build();
    }

    private BookingResponse mapToBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .bookingReference(booking.getBookingReference())
                .ride(mapToRideResponse(booking.getRide()))
                .seatsBooked(booking.getSeatsBooked())
                .totalFare(booking.getTotalFare())
                .status(booking.getStatus())
                .pickupLocation(booking.getPickupLocation())
                .dropoffLocation(booking.getDropoffLocation())
                .bookedAt(booking.getBookedAt())
                .message("Booking retrieved successfully")
                .build();
    }

    private VehicleResponse mapToVehicleResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .vehicleId(vehicle.getVehicleId())
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .year(vehicle.getYear())
                .color(vehicle.getColor())
                .licensePlate(vehicle.getLicensePlate())
                .capacity(vehicle.getCapacity())
                .type(vehicle.getType())
                .status(vehicle.getStatus())
                .vehicleInfo(vehicle.getVehicleInfo())
                .images(vehicle.getVehicleImages())
                .build();
    }
}
package com.ashu.ride_sharing.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ashu.ride_sharing.dto.request.CreateRideRequest;
import com.ashu.ride_sharing.dto.response.RideResponse;
import com.ashu.ride_sharing.dto.response.VehicleResponse;
import com.ashu.ride_sharing.models.Ride;
import com.ashu.ride_sharing.models.User;
import com.ashu.ride_sharing.models.Vehicle;
import com.ashu.ride_sharing.models.enums.RideStatus;
import com.ashu.ride_sharing.models.enums.UserRole;
import com.ashu.ride_sharing.repositories.RideRepository;
import com.ashu.ride_sharing.repositories.UserRepository;
import com.ashu.ride_sharing.repositories.VehicleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RideService {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    public RideResponse createRide(String userEmail, CreateRideRequest request){

        User driver = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Driver not found"));

        if (driver.getRole()!= UserRole.DRIVER) {
            throw new IllegalStateException("Only drivers can create rides");
        }

        if (!driver.getDriverVerified()) {
            throw new IllegalStateException("Driver must verify their driving license before creating rides");
        }

        // Check if driver has any registered vehicles
        List<Vehicle> driverVehicles = vehicleRepository.findByDriverIdAndStatus(driver.getId(), com.ashu.ride_sharing.models.enums.VehicleStatus.ACTIVE);
        if (driverVehicles.isEmpty()) {
            throw new IllegalStateException("Driver must register at least one vehicle before creating rides");
        }

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
            .orElseThrow(()-> new IllegalArgumentException("Vehicle not found"));
        
        
        if (!vehicle.getDriver().getId().equals(driver.getId())) {
            throw new IllegalArgumentException("You can only create rides with your own registered vehicles");
        }

        if (request.getDepartureDateTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Departure time must be in the future");
        }

        if (request.getAvailableSeats() > vehicle.getAvailableSeats()) {
            throw new IllegalArgumentException("Available seats cannot exceed vehicle capacity");
        }
        

        Ride ride = Ride.builder()
                .driver(driver)
                .vehicle(vehicle)
                .sourceCity(request.getSourceCity())
                .sourceAddress(request.getSourceAddress())
                .destinationCity(request.getDestinationCity())
                .destinationAddress(request.getDestinationAddress())
                .departureDateTime(request.getDepartureDateTime())
                .availableSeats(request.getAvailableSeats())
                .totalSeats(request.getAvailableSeats())
                .baseFare(request.getBaseFare())
                .description(request.getDescription())
                .status(RideStatus.ACTIVE)
                .build();

        Ride savedRide = rideRepository.save(ride);
        log.info("Ride created successfully: {} -> {} by driver {}", savedRide.getSourceCity(), savedRide.getDestinationCity(), userEmail);

        return mapToRideResponse(savedRide);

    }



    @Transactional(readOnly = true)
    public List<RideResponse> searchRides(String source, String destination, LocalDate date){
        List<Ride> rides = rideRepository.findAvailableRides(source, destination, date);
        return rides.stream()
            .map(this::mapToRideResponse)
            .toList();
    }

    @Transactional(readOnly=true)
    public List<RideResponse> getDriverRides(String userEmail){
        User driver = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new UsernameNotFoundException("Driver not found"));
        
        List<Ride> rides = rideRepository.findByDriver_IdOrderByDepartureDateTimeDesc(driver.getId());
        return rides.stream()
            .map(this::mapToRideResponse)
            .toList();
    }


    @Transactional(readOnly = true)
    public RideResponse getRideById(UUID rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));
        return mapToRideResponse(ride);
    }



     public RideResponse updateRide(String userEmail, UUID rideId, CreateRideRequest request) {
        User driver = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Driver not found"));

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        if (!ride.getDriver().getId().equals(driver.getId())) {
            throw new IllegalArgumentException("You can only update your own rides");
        }

        if (ride.getStatus() != RideStatus.ACTIVE) {
            throw new IllegalStateException("Only active rides can be updated");
        }

        ride.setSourceCity(request.getSourceCity());
        ride.setSourceAddress(request.getSourceAddress());
        ride.setDestinationCity(request.getDestinationCity());
        ride.setDestinationAddress(request.getDestinationAddress());
        ride.setDepartureDateTime(request.getDepartureDateTime());
        ride.setBaseFare(request.getBaseFare());
        ride.setDescription(request.getDescription());

        int currentBookedSeats = ride.getTotalSeats() - ride.getAvailableSeats();
        if (request.getAvailableSeats() < currentBookedSeats) {
            throw new IllegalArgumentException("Cannot reduce seats below currently booked seats");
        }
        
        ride.setTotalSeats(request.getAvailableSeats());
        ride.setAvailableSeats(request.getAvailableSeats() - currentBookedSeats);

        Ride updatedRide = rideRepository.save(ride);
        log.info("Ride updated successfully: {}", rideId);

        return mapToRideResponse(updatedRide);
    }

    public void cancelRide(String userEmail, UUID rideId, String reason) {
        User driver = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Driver not found"));

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        if (!ride.getDriver().getId().equals(driver.getId())) {
            throw new IllegalArgumentException("You can only cancel your own rides");
        }

        if (ride.getStatus() == RideStatus.CANCELLED) {
            throw new IllegalStateException("Ride is already cancelled");
        }

        if (ride.getStatus() == RideStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel completed ride");
        }

        ride.setStatus(RideStatus.CANCELLED);
        rideRepository.save(ride);

        log.info("Ride cancelled by driver {}: {} - Reason: {}", userEmail, rideId, reason);
        
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

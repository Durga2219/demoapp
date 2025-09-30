package com.ashu.ride_sharing.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ashu.ride_sharing.dto.response.DriverStatusResponse;
import com.ashu.ride_sharing.dto.response.VehicleResponse;
import com.ashu.ride_sharing.models.DriverVerificationRequest;
import com.ashu.ride_sharing.models.User;
import com.ashu.ride_sharing.models.Vehicle;
import com.ashu.ride_sharing.models.enums.UserRole;
import com.ashu.ride_sharing.models.enums.VehicleStatus;
import com.ashu.ride_sharing.repositories.UserRepository;
import com.ashu.ride_sharing.repositories.VehicleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DriverService {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    public void verifyDriver(String userEmail, DriverVerificationRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.getRole() != UserRole.DRIVER) {
            throw new IllegalStateException("User must be registered as a driver");
        }

        if (user.getDriverVerified()) {
            throw new IllegalStateException("Driver already verified");
        }

        user.setDriverLicenseNumber(request.getDriverLicenseNumber());
        user.setLicenseExpiryDate(request.getLicenseExpiryDate());
        user.setLicenseIssuingState(request.getLicenseIssuingState());
        user.setDriverVerified(true);

        // Persist driver KYC changes only. Vehicle registration is handled separately
        // via VehicleController and VehicleService, allowing multiple vehicles per driver.
        userRepository.save(user);
        log.info("Driver {} verified (license updated). Vehicle registration is separate.", userEmail);
    }

    public DriverStatusResponse getVerificationStatus(String userEmail) {
    User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    
    List<Vehicle> vehicles = vehicleRepository.findByDriverIdAndStatus(
        user.getId(), VehicleStatus.ACTIVE);
    
    return DriverStatusResponse.builder()
            .driverVerified(user.getDriverVerified())
            .driverLicenseNumber(user.getDriverLicenseNumber())
            .licenseExpiryDate(user.getLicenseExpiryDate())
            .licenseIssuingState(user.getLicenseIssuingState())
            .vehicles(vehicles.stream().map(this::mapToVehicleResponse).toList())
            .message(user.getDriverVerified() ? "Driver verified" : "Verification pending")
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

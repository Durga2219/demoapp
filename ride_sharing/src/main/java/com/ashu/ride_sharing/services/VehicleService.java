package com.ashu.ride_sharing.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ashu.ride_sharing.dto.request.VehicleRequest;
import com.ashu.ride_sharing.dto.response.VehicleResponse;
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
public class VehicleService {
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    public VehicleResponse registerVehicle(String userEmail, VehicleRequest request) {
        User driver = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Driver not found"));

        if (driver.getRole() != UserRole.DRIVER) {
            throw new IllegalStateException("Only drivers can register vehicles");
        }

        if (!driver.getDriverVerified()) {
            throw new IllegalStateException("Driver must be verified before adding vehicles");
        }

        if (vehicleRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new IllegalArgumentException("Vehicle with this license plate already exists");
        }

        Vehicle vehicle = Vehicle.builder()
                .driver(driver)
                .make(request.getMake())
                .model(request.getModel())
                .year(request.getYear())
                .color(request.getColor())
                .licensePlate(request.getLicensePlate())
                .capacity(request.getCapacity())
                .type(request.getType())
                .status(VehicleStatus.ACTIVE)
                .build();

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        log.info("Vehicle registered for driver {}: {}", userEmail, savedVehicle.getVehicleInfo());

        return mapToVehicleResponse(savedVehicle);
    }

    public List<VehicleResponse> getUserVehicles(String userEail) {
        User driver = userRepository.findByEmail(userEail)
                .orElseThrow(() -> new UsernameNotFoundException("Driver not found"));
        List<Vehicle> vehicles = vehicleRepository.findByDriverIdAndStatus(driver.getId(), VehicleStatus.ACTIVE);
        
        List<VehicleResponse> vehicleResponses = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            vehicleResponses.add(mapToVehicleResponse(vehicle));
        }

        return vehicleResponses;

        
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

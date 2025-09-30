package com.ashu.ride_sharing.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ashu.ride_sharing.models.Vehicle;
import com.ashu.ride_sharing.models.enums.VehicleStatus;


@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    List<Vehicle> findByDriverIdAndStatus(UUID driverId, VehicleStatus status);
    boolean existsByLicensePlate(String licensePlate);
    List<Vehicle> findByDriverId(UUID driverId);
}


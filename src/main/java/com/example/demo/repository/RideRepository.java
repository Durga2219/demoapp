package com.example.demo.repository;

import com.example.demo.entity.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {

    // Find rides by driver ID
    List<Ride> findByDriverId(Long driverId);

    // Find rides by status
    List<Ride> findByStatus(String status);

    // Find rides by source, destination, date and status
    List<Ride> findBySourceAndDestinationAndDateAndStatus(
            String source,
            String destination,
            LocalDate date,
            String status
    );

    // Find rides by source, destination and status (without date)
    List<Ride> findBySourceAndDestinationAndStatus(
            String source,
            String destination,
            String status
    );

    // Find rides by source and status
    List<Ride> findBySourceAndStatus(String source, String status);

    // Find rides by destination and status
    List<Ride> findByDestinationAndStatus(String destination, String status);

    // Find rides by date and status
    List<Ride> findByDateAndStatus(LocalDate date, String status);
}
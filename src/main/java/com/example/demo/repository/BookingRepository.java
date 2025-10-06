package com.example.demo.repository;

import com.example.demo.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // ---------------- PASSENGER QUERIES ----------------

    // Find all bookings by passenger
    List<Booking> findByPassengerId(Long passengerId);

    // Find bookings by passenger and status
    List<Booking> findByPassengerIdAndStatus(Long passengerId, String status);

    // Count bookings by passenger
    long countByPassengerId(Long passengerId);

    // ---------------- RIDE QUERIES ----------------

    // Find all bookings for a ride
    List<Booking> findByRideId(Long rideId);

    // Find bookings for a ride by status
    List<Booking> findByRideIdAndStatus(Long rideId, String status);

    // Find bookings with multiple statuses for a ride (e.g., ACTIVE, CONFIRMED)
    List<Booking> findByRideIdAndStatusIn(Long rideId, List<String> statuses);

    // Count bookings for a ride
    long countByRideId(Long rideId);

    // ---------------- EXISTENCE CHECK ----------------

    // Check if passenger already has an active booking for a ride
    boolean existsByPassengerIdAndRideIdAndStatus(Long passengerId, Long rideId, String status);
}

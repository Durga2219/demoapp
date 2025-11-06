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
    List<Booking> findByPassengerIdAndStatus(Long passengerId, com.example.demo.enums.BookingStatus status);

    // Count bookings by passenger
    long countByPassengerId(Long passengerId);

    // ---------------- RIDE QUERIES ----------------

    // Find all bookings for a ride
    List<Booking> findByRideId(Long rideId);

    // Find bookings for a ride by status
    List<Booking> findByRideIdAndStatus(Long rideId, com.example.demo.enums.BookingStatus status);

    // Find bookings with multiple statuses for a ride (e.g., ACTIVE, CONFIRMED)
    List<Booking> findByRideIdAndStatusIn(Long rideId, List<com.example.demo.enums.BookingStatus> statuses);

    // Count bookings for a ride
    long countByRideId(Long rideId);

    // Find bookings for a ride ordered by booking date (newest first)
    List<Booking> findByRideIdOrderByBookedAtDesc(Long rideId);

    // ---------------- EXISTENCE CHECK ----------------

    // Check if passenger already has an active booking for a ride
    boolean existsByPassengerIdAndRideIdAndStatus(Long passengerId, Long rideId, com.example.demo.enums.BookingStatus status);

    // Find bookings by ride and passenger (for review system)
    List<Booking> findByRideAndPassenger(com.example.demo.entity.Ride ride, com.example.demo.entity.User passenger);
}

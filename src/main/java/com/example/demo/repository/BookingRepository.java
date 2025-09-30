package com.example.demo.repository;

import com.example.demo.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Find bookings by passenger ID
    List<Booking> findByPassengerId(Long passengerId);

    // Find bookings by ride ID
    List<Booking> findByRideId(Long rideId);

    // Find bookings by status
    List<Booking> findByStatus(String status);

    // Find bookings by passenger ID and status
    List<Booking> findByPassengerIdAndStatus(Long passengerId, String status);

    // Find bookings by ride ID and status
    List<Booking> findByRideIdAndStatus(Long rideId, String status);
}
package com.ashu.ride_sharing.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ashu.ride_sharing.models.Booking;
import com.ashu.ride_sharing.models.enums.BookingStatus;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    
    List<Booking> findByPassenger_IdOrderByBookedAtDesc(UUID passengerId);
    
    List<Booking> findByRide_Driver_IdOrderByBookedAtDesc(UUID driverId);
    
    boolean existsByRide_RideIdAndPassenger_IdAndStatusNot(UUID rideId, UUID passengerId, BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.ride.rideId = :rideId AND b.status IN :statuses")
    List<Booking> findByRideIdAndStatusIn(@Param("rideId") UUID rideId, @Param("statuses") List<BookingStatus> statuses);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.ride.rideId = :rideId AND b.status != 'CANCELLED'")
    Long countActiveBookingsByRideId(@Param("rideId") UUID rideId);
}
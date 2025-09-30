// FILE: java/com/ashu/ride_sharing/repositories/RideRepository.java

package com.ashu.ride_sharing.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ashu.ride_sharing.models.Ride;
import com.ashu.ride_sharing.models.enums.RideStatus;

@Repository
public interface RideRepository extends JpaRepository<Ride, UUID> {
    
    // CORRECTED QUERY
    @Query("SELECT r FROM Ride r WHERE " +
           "LOWER(r.sourceCity) LIKE LOWER(CONCAT('%', :source, '%')) AND " +
           "LOWER(r.destinationCity) LIKE LOWER(CONCAT('%', :destination, '%')) AND " +
           "DATE(r.departureDateTime) = :date AND " +      
           "r.status = 'ACTIVE' AND r.availableSeats > 0 " +
           "ORDER BY r.departureDateTime ASC")            
    List<Ride> findAvailableRides(@Param("source") String source, 
                                 @Param("destination") String destination, 
                                 @Param("date") LocalDate date);

    List<Ride> findByDriver_IdOrderByDepartureDateTimeDesc(UUID driverId); 

    @Query("SELECT r FROM Ride r WHERE r.driver.id = :driverId AND r.status = :status")
    List<Ride> findByDriverIdAndStatus(@Param("driverId") UUID driverId, @Param("status") RideStatus status);
}
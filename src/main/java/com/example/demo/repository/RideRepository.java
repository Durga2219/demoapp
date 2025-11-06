package com.example.demo.repository;

import com.example.demo.entity.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {

    // ---------------- DRIVER QUERIES ----------------
    List<Ride> findByDriverId(Long driverId);

    List<Ride> findByDriverIdAndStatusOrderByDateAscTimeAsc(Long driverId, String status);

    @Query("SELECT r FROM Ride r WHERE r.driver.id = :driverId AND r.date >= :date ORDER BY r.date, r.time")
    List<Ride> findDriverUpcomingRides(@Param("driverId") Long driverId, @Param("date") LocalDate date);

    @Query("SELECT r FROM Ride r WHERE r.driver.id = :driverId AND r.date < :date ORDER BY r.date DESC, r.time DESC")
    List<Ride> findDriverPastRides(@Param("driverId") Long driverId, @Param("date") LocalDate date);

    @Query("SELECT r FROM Ride r WHERE r.driver.id = :driverId AND r.date = :date AND r.time = :time AND r.status = 'ACTIVE'")
    Optional<Ride> findConflictingRide(@Param("driverId") Long driverId,
                                       @Param("date") LocalDate date,
                                       @Param("time") java.time.LocalTime time);

    // ---------------- SEARCH QUERIES ----------------
    @Query("SELECT r FROM Ride r WHERE " +
           "LOWER(r.source) LIKE LOWER(CONCAT('%', :source, '%')) AND " +
           "LOWER(r.destination) LIKE LOWER(CONCAT('%', :destination, '%')) AND " +
           "r.date = :date AND r.status = 'ACTIVE' AND r.availableSeats >= :seats " +
           "ORDER BY r.date, r.time")
    List<Ride> searchRides(@Param("source") String source,
                           @Param("destination") String destination,
                           @Param("date") LocalDate date,
                           @Param("seats") Integer seats);

    @Query("SELECT r FROM Ride r WHERE " +
           "LOWER(r.source) LIKE LOWER(CONCAT('%', :source, '%')) AND " +
           "LOWER(r.destination) LIKE LOWER(CONCAT('%', :destination, '%')) AND " +
           "r.status = 'ACTIVE' AND r.availableSeats >= :seats " +
           "ORDER BY r.date, r.time")
    List<Ride> searchRidesFlexible(@Param("source") String source,
                                   @Param("destination") String destination,
                                   @Param("seats") Integer seats);

    // ---------------- ACTIVE RIDES ----------------
    @Query("SELECT r FROM Ride r WHERE r.status = 'ACTIVE' ORDER BY r.date, r.time")
    List<Ride> findAllActiveRides();

    // ---------------- ROUTE MATCHING ----------------
    @Query("SELECT r FROM Ride r WHERE r.date = :date AND r.availableSeats >= :seats AND r.status = 'ACTIVE' ORDER BY r.date, r.time")
    List<Ride> findByDateAndAvailableSeatsGreaterThanEqual(@Param("date") LocalDate date, @Param("seats") Integer seats);

    // ---------------- STATUS MANAGEMENT ----------------
    List<Ride> findByStatus(String status);
    
    long countByStatus(String status);

    @Query("SELECT r FROM Ride r WHERE r.status = 'ACTIVE' AND (r.date < :currentDate OR (r.date = :currentDate AND r.time < :currentTime))")
    List<Ride> findPastActiveRides(@Param("currentDate") LocalDate currentDate, @Param("currentTime") java.time.LocalTime currentTime);

    // ---------------- DASHBOARD QUERIES ----------------
    @Query("SELECT r FROM Ride r WHERE r.status = 'ACTIVE' AND r.availableSeats > 0 AND r.date >= :currentDate ORDER BY r.date ASC, r.time ASC")
    List<Ride> findAllAvailableRides(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT r FROM Ride r WHERE r.date BETWEEN :startDate AND :endDate ORDER BY r.date ASC, r.time ASC")
    List<Ride> findRidesByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // ---------------- ADMIN QUERIES ----------------
    
    long countByDate(LocalDate date);
    
    long countByDateBetween(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT r FROM Ride r ORDER BY r.createdAt DESC")
    List<Ride> findTop5ByOrderByCreatedAtDesc();
}

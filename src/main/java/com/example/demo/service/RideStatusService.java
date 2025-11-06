package com.example.demo.service;

import com.example.demo.entity.Ride;
import com.example.demo.enums.RideStatus;
import com.example.demo.repository.RideRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RideStatusService {

    private static final Logger log = LoggerFactory.getLogger(RideStatusService.class);

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Automatically update ride statuses based on current time
     * Runs every 6 hours with 15 minute initial delay
     */
    @Scheduled(fixedRate = 21600000, initialDelay = 900000) // 6 hours = 21600000ms, 15 min delay
    @Transactional
    public void updateRideStatuses() {
        try {
            LocalDate currentDate = LocalDate.now();
            LocalTime currentTime = LocalTime.now();
            
            // Find rides that should be marked as COMPLETED (past rides that are still ACTIVE)
            List<Ride> pastActiveRides = rideRepository.findPastActiveRides(currentDate, currentTime);
            
            int updatedCount = 0;
            for (Ride ride : pastActiveRides) {
                // Check if ride date is in the past, or if it's today but time has passed
                LocalDateTime rideDateTime = LocalDateTime.of(ride.getDate(), ride.getTime());
                LocalDateTime currentDateTime = LocalDateTime.now();
                
                // Add 2 hours buffer after ride time to mark as completed
                if (rideDateTime.plusHours(2).isBefore(currentDateTime)) {
                    String oldStatus = ride.getStatus();
                    ride.setStatus("COMPLETED");
                    rideRepository.save(ride);
                    updatedCount++;
                    log.info("Updated ride {} to COMPLETED status", ride.getId());

                    // Broadcast ride status update
                    broadcastRideStatusUpdate(ride, oldStatus, "COMPLETED");
                }
            }
            
            if (updatedCount > 0) {
                log.info("Updated {} rides to COMPLETED status", updatedCount);
            }
            
        } catch (Exception e) {
            log.error("Error updating ride statuses: {}", e.getMessage(), e);
        }
    }

    /**
     * Manually update ride statuses (can be called from API)
     */
    @Transactional
    public int updateRideStatusesManually() {
        try {
            LocalDate currentDate = LocalDate.now();
            LocalTime currentTime = LocalTime.now();
            
            List<Ride> pastActiveRides = rideRepository.findPastActiveRides(currentDate, currentTime);
            
            int updatedCount = 0;
            for (Ride ride : pastActiveRides) {
                LocalDateTime rideDateTime = LocalDateTime.of(ride.getDate(), ride.getTime());
                LocalDateTime currentDateTime = LocalDateTime.now();
                
                if (rideDateTime.plusHours(2).isBefore(currentDateTime)) {
                    ride.setStatus("COMPLETED");
                    rideRepository.save(ride);
                    updatedCount++;
                }
            }
            
            log.info("Manually updated {} rides to COMPLETED status", updatedCount);
            return updatedCount;
            
        } catch (Exception e) {
            log.error("Error manually updating ride statuses: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Get ride statistics
     */
    public RideStats getRideStats() {
        try {
            long totalRides = rideRepository.count();
            long activeRides = rideRepository.countByStatus("ACTIVE");
            long completedRides = rideRepository.countByStatus("COMPLETED");
            long cancelledRides = rideRepository.countByStatus("CANCELLED");
            
            return new RideStats(totalRides, activeRides, completedRides, cancelledRides);
        } catch (Exception e) {
            log.error("Error getting ride stats: {}", e.getMessage(), e);
            return new RideStats(0, 0, 0, 0);
        }
    }

    // Inner class for ride statistics
    public static class RideStats {
        private final long totalRides;
        private final long activeRides;
        private final long completedRides;
        private final long cancelledRides;

        public RideStats(long totalRides, long activeRides, long completedRides, long cancelledRides) {
            this.totalRides = totalRides;
            this.activeRides = activeRides;
            this.completedRides = completedRides;
            this.cancelledRides = cancelledRides;
        }

        // Getters
        public long getTotalRides() { return totalRides; }
        public long getActiveRides() { return activeRides; }
        public long getCompletedRides() { return completedRides; }
        public long getCancelledRides() { return cancelledRides; }
    }

    /**
     * Broadcast ride status update to relevant users
     */
    private void broadcastRideStatusUpdate(Ride ride, String oldStatus, String newStatus) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("rideId", ride.getId());
            update.put("oldStatus", oldStatus);
            update.put("newStatus", newStatus);
            update.put("timestamp", LocalDateTime.now());
            update.put("source", ride.getSource());
            update.put("destination", ride.getDestination());
            update.put("driverName", ride.getDriver().getName());

            // Send to driver
            messagingTemplate.convertAndSendToUser(
                ride.getDriver().getUsername(),
                "/queue/ride-status-updates",
                update
            );

            // Send to passengers (from bookings)
            List<com.example.demo.entity.Booking> bookings = ride.getBookings();
            if (bookings != null) {
                for (com.example.demo.entity.Booking booking : bookings) {
                    if (booking.getStatus() == com.example.demo.enums.BookingStatus.CONFIRMED) {
                        messagingTemplate.convertAndSendToUser(
                            booking.getPassenger().getUsername(),
                            "/queue/ride-status-updates",
                            update
                        );
                    }
                }
            }

            log.info("Broadcasted ride status update for ride {}: {} -> {}", ride.getId(), oldStatus, newStatus);

        } catch (Exception e) {
            log.error("Error broadcasting ride status update: {}", e.getMessage(), e);
        }
    }
}

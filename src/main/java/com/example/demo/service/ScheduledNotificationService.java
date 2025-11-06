package com.example.demo.service;

import com.example.demo.entity.Booking;
import com.example.demo.entity.Ride;
import com.example.demo.enums.BookingStatus;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.RideRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ScheduledNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledNotificationService.class);

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RideRepository rideRepository;

    /**
     * Send ride reminders ~5 minutes before ride time
     * TEST MODE: Runs every 1 minute with 10s initial delay for quick verification
     */
    @Scheduled(fixedRate = 60000, initialDelay = 10000) // 1 min, 10s delay
    public void sendRideReminders() {
        try {
            logger.info("REMINDER: Checking for rides that need reminders...");

            LocalDateTime now = LocalDateTime.now();
            // Target window: rides starting around 5 minutes from now (±1 minute)
            LocalDateTime reminderWindowStart = now.plusMinutes(4);
            LocalDateTime reminderWindowEnd = now.plusMinutes(6);
            
            // Get today's date
            LocalDate todayDate = LocalDate.now();
            
            // Find rides happening today
            List<Ride> todayRides = rideRepository.findByDateAndAvailableSeatsGreaterThanEqual(todayDate, 0);
            
            for (Ride ride : todayRides) {
                try {
                    // Parse ride time
                    LocalTime rideTime = ride.getTime();
                    LocalDateTime rideDateTime = ride.getDate().atTime(rideTime);
                    
                    // Check if ride is approximately 5 minutes away (±1 minute)
                    boolean inWindow = !rideDateTime.isBefore(reminderWindowStart) && !rideDateTime.isAfter(reminderWindowEnd);
                    if (inWindow) {
                        // Send reminders for all confirmed bookings of this ride
                        List<Booking> confirmedBookings = bookingRepository.findByRideIdAndStatus(ride.getId(), BookingStatus.CONFIRMED);
                        
                        if (!confirmedBookings.isEmpty()) {
                            notificationService.sendRideReminderNotification(ride);
                            logger.info("📧 Ride reminder sent for ride: {} with {} bookings (5-min window)", ride.getId(), confirmedBookings.size());
                        }
                        
                        if (!confirmedBookings.isEmpty()) {
                            logger.info("✅ Sent {} ride reminders for ride: {}", confirmedBookings.size(), ride.getId());
                        }
                    }
                    
                } catch (Exception e) {
                    logger.error("Error processing ride reminder for ride {}: {}", ride.getId(), e.getMessage());
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in scheduled ride reminder service: {}", e.getMessage(), e);
        }
    }

    /**
     * Clean up old notifications (older than 30 days)
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldNotifications() {
        try {
            logger.info("CLEANUP: Cleaning up old notifications...");
            
            // This would require a method in NotificationRepository to delete old notifications
            // For now, we'll just log the intent
            logger.info("NOTE: Implement notification cleanup in NotificationRepository");
            
        } catch (Exception e) {
            logger.error("Error cleaning up old notifications: {}", e.getMessage(), e);
        }
    }

    /**
     * Send review requests 1 hour after ride completion
     * Runs every 4 hours with 10 minute initial delay
     */
    @Scheduled(fixedRate = 14400000, initialDelay = 600000) // 4 hours = 14400000ms, 10 min delay
    public void sendReviewRequests() {
        try {
            logger.info("REVIEW: Checking for completed rides that need review requests...");

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime oneHourAgo = now.minusHours(1);
            
            // Find rides that completed approximately 1 hour ago
            LocalDate todayDate = LocalDate.now();
            List<Ride> todayRides = rideRepository.findByDateAndAvailableSeatsGreaterThanEqual(todayDate, 0);
            
            for (Ride ride : todayRides) {
                try {
                    LocalTime rideTime = ride.getTime();
                    LocalDateTime rideDateTime = ride.getDate().atTime(rideTime);
                    
                    // Assume ride duration is 2 hours (you can make this configurable)
                    LocalDateTime estimatedCompletionTime = rideDateTime.plusHours(2);
                    
                    // Check if ride completed approximately 1 hour ago
                    long minutesSinceCompletion = java.time.Duration.between(estimatedCompletionTime, now).toMinutes();
                    
                    if (minutesSinceCompletion >= 30 && minutesSinceCompletion <= 90) { // 30 minutes to 1.5 hours after completion
                        List<Booking> confirmedBookings = bookingRepository.findByRideIdAndStatus(ride.getId(), BookingStatus.CONFIRMED);
                        
                        for (Booking booking : confirmedBookings) {
                            // Check if reviews already exist to avoid duplicate requests
                            // This would require checking the ReviewRepository
                            notificationService.sendReviewRequestNotification(booking);
                            logger.info("REVIEW: Review request sent for booking: {}", booking.getId());
                        }
                        
                        if (!confirmedBookings.isEmpty()) {
                            logger.info("✅ Sent {} review requests for completed ride: {}", confirmedBookings.size(), ride.getId());
                        }
                    }
                    
                } catch (Exception e) {
                    logger.error("Error processing review request for ride {}: {}", ride.getId(), e.getMessage());
                }
            }
            
        } catch (Exception e) {
            logger.error("Error in scheduled review request service: {}", e.getMessage(), e);
        }
    }
}
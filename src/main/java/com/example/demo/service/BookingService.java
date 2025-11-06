package com.example.demo.service;

import com.example.demo.entity.Booking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BookingService {

    @Autowired
    private RideService rideService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Get booking by ID
     */
    public Optional<Booking> getBookingById(Long bookingId) {
        try {
            Booking booking = rideService.getBookingById(bookingId);
            return Optional.of(booking);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * Confirm booking after successful payment
     */
    public Booking confirmBookingAfterPayment(Long bookingId) {
        Booking booking = rideService.confirmBookingAfterPayment(bookingId);
        
        // Send booking confirmation notifications
        try {
            notificationService.sendBookingConfirmationNotification(booking);
        } catch (Exception e) {
            System.err.println("Failed to send booking confirmation notifications: " + e.getMessage());
        }
        
        return booking;
    }

    /**
     * Complete booking and send review request
     */
    public void completeBooking(Long bookingId) {
        try {
            Optional<Booking> bookingOpt = getBookingById(bookingId);
            if (bookingOpt.isPresent()) {
                Booking booking = bookingOpt.get();
                // Send review request notifications
                notificationService.sendReviewRequestNotification(booking);
            }
        } catch (Exception e) {
            System.err.println("Failed to send review request notifications: " + e.getMessage());
        }
    }
}
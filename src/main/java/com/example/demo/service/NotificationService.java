package com.example.demo.service;

import com.example.demo.controller.WebSocketNotificationController;
import com.example.demo.dto.NotificationResponse;
import com.example.demo.entity.Notification;
import com.example.demo.entity.NotificationType;
import com.example.demo.entity.User;
import com.example.demo.entity.Booking;
import com.example.demo.entity.Ride;
import com.example.demo.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ApplicationContext applicationContext;

    private WebSocketNotificationController webSocketController;

    // Lazy initialization to avoid circular dependency
    private WebSocketNotificationController getWebSocketController() {
        if (webSocketController == null) {
            try {
                webSocketController = applicationContext.getBean(WebSocketNotificationController.class);
            } catch (Exception e) {
                System.err.println("WebSocket controller not available: " + e.getMessage());
            }
        }
        return webSocketController;
    }

    // Create and save notification
    public Notification createNotification(User user, String title, String message, NotificationType type) {
        Notification notification = new Notification(user, title, message, type);
        Notification savedNotification = notificationRepository.save(notification);
        
        // Send real-time notification via WebSocket
        try {
            WebSocketNotificationController wsController = getWebSocketController();
            if (wsController != null) {
                NotificationResponse notificationResponse = new NotificationResponse(savedNotification);
                wsController.sendNotificationToUser(user.getUsername(), notificationResponse);
                
                // Update unread count
                Long unreadCount = getUnreadCount(user);
                wsController.sendUnreadCountToUser(user.getUsername(), unreadCount);
            }
        } catch (Exception e) {
            System.err.println("Failed to send real-time notification: " + e.getMessage());
        }
        
        return savedNotification;
    }

    // Create notification with ride/booking reference
    public Notification createNotification(User user, String title, String message, NotificationType type, Long rideId, Long bookingId) {
        Notification notification = new Notification(user, title, message, type);
        notification.setRideId(rideId);
        notification.setBookingId(bookingId);
        Notification savedNotification = notificationRepository.save(notification);
        
        // Send real-time notification via WebSocket
        try {
            WebSocketNotificationController wsController = getWebSocketController();
            if (wsController != null) {
                NotificationResponse notificationResponse = new NotificationResponse(savedNotification);
                wsController.sendNotificationToUser(user.getUsername(), notificationResponse);
                
                // Update unread count
                Long unreadCount = getUnreadCount(user);
                wsController.sendUnreadCountToUser(user.getUsername(), unreadCount);
            }
        } catch (Exception e) {
            System.err.println("Failed to send real-time notification: " + e.getMessage());
        }
        
        return savedNotification;
    }

    // Get user notifications
    public List<Notification> getUserNotifications(User user) {
        return notificationRepository.findTop10ByUserOrderByCreatedAtDesc(user);
    }

    // Get unread notifications count
    public Long getUnreadCount(User user) {
        return notificationRepository.countUnreadNotifications(user);
    }

    // Mark notification as read
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setIsRead(true);
            notificationRepository.save(notification);
            
            // Update unread count via WebSocket
            try {
                WebSocketNotificationController wsController = getWebSocketController();
                if (wsController != null) {
                    Long unreadCount = getUnreadCount(notification.getUser());
                    wsController.sendUnreadCountToUser(notification.getUser().getUsername(), unreadCount);
                }
            } catch (Exception e) {
                System.err.println("Failed to send unread count update: " + e.getMessage());
            }
        });
    }

    // Mark all notifications as read for user
    public void markAllAsRead(User user) {
        List<Notification> unreadNotifications = notificationRepository.findByUserAndIsReadOrderByCreatedAtDesc(user, false);
        unreadNotifications.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(unreadNotifications);
        
        // Update unread count via WebSocket
        try {
            WebSocketNotificationController wsController = getWebSocketController();
            if (wsController != null) {
                wsController.sendUnreadCountToUser(user.getUsername(), 0L);
            }
        } catch (Exception e) {
            System.err.println("Failed to send unread count update: " + e.getMessage());
        }
    }

    // Specific notification methods for different events
    public void sendBookingConfirmationNotification(Booking booking) {
        // Notify passenger
        createNotification(
            booking.getPassenger(),
            "Booking Confirmed!",
            String.format("Your ride from %s to %s has been confirmed for %s", 
                booking.getPickupLocation(), booking.getDropLocation(), booking.getRide().getDate()),
            NotificationType.BOOKING_CONFIRMATION,
            booking.getRide().getId(),
            booking.getId()
        );

        // Notify driver
        createNotification(
            booking.getRide().getDriver(),
            "New Booking Received!",
            String.format("You have a new booking from %s for %d seat(s)", 
                booking.getPassenger().getUsername(), booking.getSeatsBooked()),
            NotificationType.BOOKING_CONFIRMATION,
            booking.getRide().getId(),
            booking.getId()
        );

        // Send email notifications
        try {
            emailService.sendBookingConfirmationEmail(booking);
        } catch (Exception e) {
            System.err.println("Failed to send email notification: " + e.getMessage());
        }
    }

    public void sendRideReminderNotification(Ride ride) {
        // Notify driver
        createNotification(
            ride.getDriver(),
            "Ride Reminder 🕐",
            String.format("Your ride from %s to %s is scheduled for today at %s", 
                ride.getSource(), ride.getDestination(), ride.getTime()),
            NotificationType.RIDE_REMINDER,
            ride.getId(),
            null
        );
        // Email driver
        try { emailService.sendRideReminderEmail(ride, ride.getDriver(), true); } catch (Exception ignored) {}

        // Notify all passengers with bookings for this ride
        ride.getBookings().forEach(booking -> {
            if ("CONFIRMED".equals(booking.getStatus())) {
                createNotification(
                    booking.getPassenger(),
                    "Ride Reminder 🚗",
                    String.format("Your ride from %s to %s is scheduled for today at %s", 
                        ride.getSource(), ride.getDestination(), ride.getTime()),
                    NotificationType.RIDE_REMINDER,
                    ride.getId(),
                    booking.getId()
                );
                // Email passenger
                try { emailService.sendRideReminderEmail(ride, booking.getPassenger(), false); } catch (Exception ignored) {}
            }
        });
    }

    public void sendRideCancellationNotification(Ride ride) {
        // Notify all passengers with bookings
        ride.getBookings().forEach(booking -> {
            if ("CONFIRMED".equals(booking.getStatus())) {
                createNotification(
                    booking.getPassenger(),
                    "Ride Cancelled ❌",
                    String.format("Unfortunately, the ride from %s to %s scheduled for %s has been cancelled by the driver", 
                        ride.getSource(), ride.getDestination(), ride.getDate()),
                    NotificationType.RIDE_CANCELLED,
                    ride.getId(),
                    booking.getId()
                );
                // Email passenger
                try {
                    emailService.sendRideUpdateEmail(
                        ride,
                        booking.getPassenger(),
                        "Ride Cancelled",
                        String.format("Your ride from %s to %s on %s was cancelled.", ride.getSource(), ride.getDestination(), ride.getDate())
                    );
                } catch (Exception ignored) {}
            }
        });
    }

    public void sendPaymentNotification(User user, String message, boolean isSuccess) {
        createNotification(
            user,
            isSuccess ? "Payment Successful ✅" : "Payment Failed ❌",
            message,
            isSuccess ? NotificationType.PAYMENT_SUCCESS : NotificationType.PAYMENT_FAILED
        );
        // Email user
        try { emailService.sendPaymentEmail(user, message, isSuccess); } catch (Exception ignored) {}
    }

    public void sendReviewRequestNotification(Booking booking) {
        // Request review from passenger
        createNotification(
            booking.getPassenger(),
            "Rate Your Ride Experience ⭐",
            String.format("How was your ride with %s? Please share your feedback", 
                booking.getRide().getDriver().getUsername()),
            NotificationType.REVIEW_REQUEST,
            booking.getRide().getId(),
            booking.getId()
        );
        try { emailService.sendReviewRequestEmail(booking, false); } catch (Exception ignored) {}

        // Request review from driver
        createNotification(
            booking.getRide().getDriver(),
            "Rate Your Passenger ⭐",
            String.format("How was your experience with %s? Please share your feedback", 
                booking.getPassenger().getUsername()),
            NotificationType.REVIEW_REQUEST,
            booking.getRide().getId(),
            booking.getId()
        );
        try { emailService.sendReviewRequestEmail(booking, true); } catch (Exception ignored) {}
    }

    public void sendReviewReceivedNotification(com.example.demo.entity.Review review) {
        String reviewerName = review.getReviewer().getUsername();
        String message = String.format("%s has rated you %d star%s%s", 
            reviewerName, 
            review.getRating(), 
            review.getRating() == 1 ? "" : "s",
            review.getComment() != null && !review.getComment().trim().isEmpty() ? 
                " and left a comment" : "");
        
        createNotification(
            review.getReviewee(),
            "New Review Received ⭐",
            message,
            NotificationType.REVIEW_RECEIVED,
            review.getBooking().getRide().getId(),
            review.getBooking().getId()
        );
        // Email reviewee
        try { emailService.sendReviewReceivedEmail(review); } catch (Exception ignored) {}
    }
}
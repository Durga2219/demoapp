package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:test@example.com}")
    private String fromEmail;

    @Value("${email.test.mode:true}")
    private boolean testMode;

    /**
     * Send OTP email
     */
    public void sendOtpEmail(String toEmail, String otp, String purpose) {
        try {
            if (testMode) {
                // Test mode - log OTP instead of sending email
                logger.info("🧪 TEST MODE - OTP for {}: {} (Purpose: {})", toEmail, otp, purpose);
                logger.info("📧 In production, this OTP would be sent to: {}", toEmail);
                return;
            }

            if (mailSender == null) {
                logger.error("❌ JavaMailSender not configured! Cannot send real emails.");
                logger.info("🧪 FALLBACK TEST MODE - OTP for {}: {} (Purpose: {})", toEmail, otp, purpose);
                throw new RuntimeException("Email service not properly configured. Check Gmail credentials.");
            }

            logger.info("📧 Sending real email to: {} (Purpose: {})", toEmail, purpose);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Dynamic Ride Sharing - " + purpose + " OTP");
            
            String emailBody = buildOtpEmailBody(otp, purpose);
            message.setText(emailBody);

            // Send the email
            mailSender.send(message);
            
            logger.info("✅ OTP email sent successfully to: {}", toEmail);
            logger.info("📬 Email delivered! Check inbox (and spam folder) for: {}", toEmail);

        } catch (Exception e) {
            if (testMode) {
                // In test mode, don't fail on email errors
                logger.warn("Test mode: Email sending simulated for: {}", toEmail);
                return;
            }
            
            logger.error("❌ Failed to send OTP email to: {}", toEmail, e);
            
            // Provide helpful error messages
            String errorMsg = e.getMessage();
            if (errorMsg.contains("Authentication failed")) {
                throw new RuntimeException("Gmail authentication failed. Please check your app password in application.properties");
            } else if (errorMsg.contains("Connection")) {
                throw new RuntimeException("Cannot connect to Gmail SMTP. Check internet connection and Gmail settings.");
            } else {
                throw new RuntimeException("Failed to send email: " + errorMsg);
            }
        }
    }

    /**
     * Build OTP email body
     */
    private String buildOtpEmailBody(String otp, String purpose) {
        return String.format(
            "🚗 Dynamic Ride Sharing - %s\n\n" +
            "Hello!\n\n" +
            "Your verification code is: %s\n\n" +
            "⏰ This code expires in 5 minutes\n" +
            "🔒 Keep this code secure - never share it with anyone\n" +
            "📱 Enter this code in the app to continue\n\n" +
            "If you didn't request this code, please ignore this email.\n\n" +
            "Need help? Contact our support team.\n\n" +
            "Best regards,\n" +
            "Dynamic Ride Sharing Team\n" +
            "🌐 Making transportation smarter and greener",
            purpose, otp
        );
    }

    /**
     * Send welcome email after successful registration
     */
    public void sendWelcomeEmail(String toEmail, String userName) {
        try {
            if (testMode) {
                logger.info("🧪 TEST MODE - Welcome email would be sent to: {} for user: {}", toEmail, userName);
                return;
            }

            if (mailSender == null) {
                logger.warn("JavaMailSender not configured, skipping welcome email");
                return;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to Dynamic Ride Sharing!");
            
            String emailBody = String.format(
                "Hello %s,\n\n" +
                "Welcome to Dynamic Ride Sharing!\n\n" +
                "Your account has been successfully created. You can now:\n" +
                "• Book rides as a passenger\n" +
                "• Offer rides as a driver\n" +
                "• Manage your wallet and earnings\n\n" +
                "Thank you for joining us!\n\n" +
                "Best regards,\n" +
                "Dynamic Ride Sharing Team",
                userName
            );
            
            message.setText(emailBody);
            mailSender.send(message);
            logger.info("Welcome email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            logger.error("Failed to send welcome email to: {}", toEmail, e);
            // Don't throw exception for welcome email failure
        }
    }

    /**
     * Send booking confirmation email
     */
    public void sendBookingConfirmationEmail(com.example.demo.entity.Booking booking) {
        try {
            if (testMode) {
                logger.info("🧪 TEST MODE - Booking confirmation email for booking ID: {}", booking.getId());
                logger.info("📧 Passenger: {} | Driver: {}", 
                    booking.getPassenger().getEmail(), 
                    booking.getRide().getDriver().getEmail());
                return;
            }

            if (mailSender == null) {
                logger.error("❌ JavaMailSender not configured! Cannot send booking confirmation emails.");
                return;
            }

            // Send email to passenger
            sendBookingEmailToPassenger(booking);
            
            // Send email to driver
            sendBookingEmailToDriver(booking);
            
        } catch (Exception e) {
            logger.error("❌ Failed to send booking confirmation emails for booking ID: {}", booking.getId(), e);
        }
    }

    private void sendBookingEmailToPassenger(com.example.demo.entity.Booking booking) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(booking.getPassenger().getEmail());
        message.setSubject("🎉 Ride Booking Confirmed - Dynamic Ride Sharing");
        
        String emailBody = String.format(
            "🚗 Your Ride is Confirmed!\n\n" +
            "Hi %s,\n\n" +
            "Great news! Your ride booking has been confirmed.\n\n" +
            "📋 BOOKING DETAILS:\n" +
            "🆔 Booking ID: #%d\n" +
            "📍 From: %s\n" +
            "📍 To: %s\n" +
            "📅 Date: %s\n" +
            "🕐 Time: %s\n" +
            "👥 Seats: %d\n" +
            "💰 Fare: ₹%.2f\n\n" +
            "🚗 DRIVER DETAILS:\n" +
            "👤 Driver: %s\n" +
            "📧 Email: %s\n" +
            "🚙 Vehicle: %s\n\n" +
            "📱 What's Next?\n" +
            "• Be ready 10 minutes before departure\n" +
            "• Contact your driver if needed\n" +
            "• Have a safe journey!\n\n" +
            "Need help? Contact our support team.\n\n" +
            "Happy travels! 🌟\n" +
            "Dynamic Ride Sharing Team",
            booking.getPassenger().getUsername(),
            booking.getId(),
            booking.getPickupLocation(),
            booking.getDropLocation(),
            booking.getRide().getDate(),
            booking.getRide().getTime(),
            booking.getSeatsBooked(),
            booking.getFare(),
            booking.getRide().getDriver().getUsername(),
            booking.getRide().getDriver().getEmail(),
            booking.getRide().getVehicleModel() != null ? booking.getRide().getVehicleModel() : "Vehicle"
        );
        
        message.setText(emailBody);
        mailSender.send(message);
        logger.info("✅ Booking confirmation email sent to passenger: {}", booking.getPassenger().getEmail());
    }

    /**
     * Send ride reminder email
     */
    public void sendRideReminderEmail(com.example.demo.entity.Ride ride, com.example.demo.entity.User user, boolean isDriver) {
        try {
            if (testMode) {
                logger.info("🧪 TEST MODE - Ride reminder email for user: {} ({})", user.getEmail(), isDriver ? "Driver" : "Passenger");
                return;
            }

            if (mailSender == null) {
                logger.error("❌ JavaMailSender not configured! Cannot send ride reminder emails.");
                return;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("🕐 Ride Reminder - Your ride is today!");
            
            String emailBody = String.format(
                "🚗 Ride Reminder!\n\n" +
                "Hi %s,\n\n" +
                "This is a friendly reminder that you have a ride scheduled for today.\n\n" +
                "📋 RIDE DETAILS:\n" +
                "🆔 Ride ID: #%d\n" +
                "📍 From: %s\n" +
                "📍 To: %s\n" +
                "📅 Date: %s\n" +
                "🕐 Time: %s\n" +
                "🚙 Vehicle: %s\n" +
                "👥 Available Seats: %d\n\n" +
                "%s\n\n" +
                "📱 What's Next?\n" +
                "• Be ready 10 minutes before departure\n" +
                "• Check all ride details in your dashboard\n" +
                "• Have a safe journey!\n\n" +
                "Safe travels! 🌟\n" +
                "Dynamic Ride Sharing Team",
                user.getUsername(),
                ride.getId(),
                ride.getSource(),
                ride.getDestination(),
                ride.getDate(),
                ride.getTime(),
                ride.getVehicleModel() != null ? ride.getVehicleModel() : "Vehicle",
                ride.getAvailableSeats(),
                isDriver ? "As the driver, please ensure you arrive on time and provide a safe ride for your passengers." : "Your driver will pick you up at the scheduled time. Please be punctual."
            );
            
            message.setText(emailBody);
            mailSender.send(message);
            logger.info("✅ Ride reminder email sent to: {}", user.getEmail());

        } catch (Exception e) {
            logger.error("❌ Failed to send ride reminder email to: {}", user.getEmail(), e);
        }
    }

    /**
     * Send ride update email (for cancellations, changes, etc.)
     */
    public void sendRideUpdateEmail(com.example.demo.entity.Ride ride, com.example.demo.entity.User user, String updateType, String updateMessage) {
        try {
            if (testMode) {
                logger.info("🧪 TEST MODE - Ride update email ({}) for user: {}", updateType, user.getEmail());
                return;
            }

            if (mailSender == null) {
                logger.error("❌ JavaMailSender not configured! Cannot send ride update emails.");
                return;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("📢 Ride Update - " + updateType);
            
            String emailBody = String.format(
                "📢 Ride Update\n\n" +
                "Hi %s,\n\n" +
                "There has been an update to your ride.\n\n" +
                "📋 RIDE DETAILS:\n" +
                "🆔 Ride ID: #%d\n" +
                "📍 From: %s\n" +
                "📍 To: %s\n" +
                "📅 Date: %s\n" +
                "🕐 Time: %s\n\n" +
                "📝 UPDATE:\n" +
                "%s\n\n" +
                "If you have any questions or concerns, please contact support.\n\n" +
                "Best regards,\n" +
                "Dynamic Ride Sharing Team",
                user.getUsername(),
                ride.getId(),
                ride.getSource(),
                ride.getDestination(),
                ride.getDate(),
                ride.getTime(),
                updateMessage
            );
            
            message.setText(emailBody);
            mailSender.send(message);
            logger.info("✅ Ride update email sent to: {}", user.getEmail());

        } catch (Exception e) {
            logger.error("❌ Failed to send ride update email to: {}", user.getEmail(), e);
        }
    }

    /**
     * Send review request email
     */
    public void sendReviewRequestEmail(com.example.demo.entity.Booking booking, boolean toDriver) {
        try {
            if (testMode) {
                logger.info("🧪 TEST MODE - Review request email for booking ID: {}", booking.getId());
                return;
            }

            if (mailSender == null) {
                logger.error("❌ JavaMailSender not configured! Cannot send review request emails.");
                return;
            }

            com.example.demo.entity.User recipient = toDriver ? booking.getRide().getDriver() : booking.getPassenger();
            com.example.demo.entity.User reviewee = toDriver ? booking.getPassenger() : booking.getRide().getDriver();

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(recipient.getEmail());
            message.setSubject("⭐ Rate Your Ride Experience");
            
            String emailBody = String.format(
                "⭐ Rate Your Ride Experience\n\n" +
                "Hi %s,\n\n" +
                "Thank you for using Dynamic Ride Sharing!\n\n" +
                "We hope you had a great experience on your recent ride.\n" +
                "Please take a moment to rate your %s, %s.\n\n" +
                "📋 RIDE DETAILS:\n" +
                "🆔 Booking ID: #%d\n" +
                "📍 Route: %s → %s\n" +
                "📅 Date: %s\n\n" +
                "Your feedback helps us maintain quality and improve our service.\n\n" +
                "Login to your dashboard to submit your review!\n\n" +
                "Thank you for your time! 🌟\n" +
                "Dynamic Ride Sharing Team",
                recipient.getUsername(),
                toDriver ? "passenger" : "driver",
                reviewee.getUsername(),
                booking.getId(),
                booking.getPickupLocation(),
                booking.getDropLocation(),
                booking.getRide().getDate()
            );
            
            message.setText(emailBody);
            mailSender.send(message);
            logger.info("✅ Review request email sent to: {}", recipient.getEmail());

        } catch (Exception e) {
            logger.error("❌ Failed to send review request email", e);
        }
    }

    private void sendBookingEmailToDriver(com.example.demo.entity.Booking booking) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(booking.getRide().getDriver().getEmail());
        message.setSubject("🚗 New Passenger Booking - Dynamic Ride Sharing");
        
        String emailBody = String.format(
            "🎉 New Booking Received!\n\n" +
            "Hi %s,\n\n" +
            "You have a new passenger booking for your ride.\n\n" +
            "📋 BOOKING DETAILS:\n" +
            "🆔 Booking ID: #%d\n" +
            "📍 Route: %s → %s\n" +
            "📅 Date: %s\n" +
            "🕐 Time: %s\n" +
            "👥 Seats Booked: %d\n" +
            "💰 Earnings: ₹%.2f\n\n" +
            "👤 PASSENGER DETAILS:\n" +
            "👤 Name: %s\n" +
            "📧 Email: %s\n" +
            "📞 Phone: %s\n" +
            "📍 Pickup: %s\n" +
            "📍 Drop: %s\n\n" +
            "📱 What's Next?\n" +
            "• Contact passenger if needed\n" +
            "• Be punctual and professional\n" +
            "• Ensure a safe journey\n\n" +
            "Keep up the great work! 🌟\n" +
            "Dynamic Ride Sharing Team",
            booking.getRide().getDriver().getUsername(),
            booking.getId(),
            booking.getRide().getSource(),
            booking.getRide().getDestination(),
            booking.getRide().getDate(),
            booking.getRide().getTime(),
            booking.getSeatsBooked(),
            booking.getFare(),
            booking.getPassenger().getUsername(),
            booking.getPassenger().getEmail(),
            booking.getPassenger().getPhone() != null ? booking.getPassenger().getPhone() : "Not provided",
            booking.getPickupLocation(),
            booking.getDropLocation()
        );
        
        message.setText(emailBody);
        mailSender.send(message);
        logger.info("✅ Booking notification email sent to driver: {}", booking.getRide().getDriver().getEmail());
    }

    /**
     * Send generic payment status email
     */
    public void sendPaymentEmail(com.example.demo.entity.User user, String messageText, boolean isSuccess) {
        try {
            if (testMode) {
                logger.info("🧪 TEST MODE - Payment email (success={}) to {}", isSuccess, user.getEmail());
                return;
            }
            if (mailSender == null) {
                logger.error("❌ JavaMailSender not configured! Cannot send payment emails.");
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject((isSuccess ? "✅ Payment Successful" : "❌ Payment Failed") + " - Dynamic Ride Sharing");
            message.setText(
                (isSuccess ? "Your payment was processed successfully.\n\n" : "Your payment could not be processed.\n\n") +
                (messageText != null ? messageText : "") +
                "\n\nThank you,\nDynamic Ride Sharing Team"
            );
            mailSender.send(message);
            logger.info("✅ Payment email sent to: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("❌ Failed to send payment email to: {}", user.getEmail(), e);
        }
    }

    /**
     * Send email when a new review is received
     */
    public void sendReviewReceivedEmail(com.example.demo.entity.Review review) {
        try {
            if (testMode) {
                logger.info("🧪 TEST MODE - Review received email to {}", review.getReviewee().getEmail());
                return;
            }
            if (mailSender == null) {
                logger.error("❌ JavaMailSender not configured! Cannot send review received emails.");
                return;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(review.getReviewee().getEmail());
            message.setSubject("⭐ New Review Received - Dynamic Ride Sharing");

            String body = String.format(
                "Hi %s,\n\n" +
                "You received a new review from %s.\n\n" +
                "Rating: %d/5\n" +
                "Comment: %s\n\n" +
                "You can view more details in your dashboard.\n\n" +
                "Thanks,\nDynamic Ride Sharing Team",
                review.getReviewee().getUsername(),
                review.getReviewer().getUsername(),
                review.getRating(),
                review.getComment() != null ? review.getComment() : "(no comment)"
            );

            message.setText(body);
            mailSender.send(message);
            logger.info("✅ Review received email sent to: {}", review.getReviewee().getEmail());
        } catch (Exception e) {
            logger.error("❌ Failed to send review received email", e);
        }
    }
}
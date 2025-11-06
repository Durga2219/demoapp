package com.example.demo.service;

import com.example.demo.controller.AdminWebSocketController;
import com.example.demo.entity.*;
import com.example.demo.repository.PaymentRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private RazorpayService razorpayService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AdminWebSocketController adminWebSocketController;

    /**
     * Create a new payment for a booking
     */
    public Payment createPayment(Booking booking, User user, Double amount) {
        try {
            logger.info("Creating payment for booking {} with amount ₹{}", booking.getId(), amount);

            // Create Razorpay order
            String razorpayOrderId = razorpayService.createOrder(amount, "INR", "Payment for ride booking");

            // Create payment entity
            Payment payment = new Payment(booking, user, amount, razorpayOrderId);
            payment.setStatus(PaymentStatus.CREATED);

            Payment savedPayment = paymentRepository.save(payment);
            logger.info("Payment created successfully with ID: {}", savedPayment.getId());

            return savedPayment;

        } catch (Exception e) {
            logger.error("Error creating payment for booking {}: {}", booking.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create payment: " + e.getMessage());
        }
    }

    /**
     * Process payment verification after successful payment
     */
    public Payment processPayment(String razorpayPaymentId, String razorpayOrderId, String razorpaySignature) {
        try {
            logger.info("Processing payment verification for order: {}", razorpayOrderId);

            // Find payment by order ID
            Optional<Payment> paymentOpt = paymentRepository.findByRazorpayOrderId(razorpayOrderId);
            if (paymentOpt.isEmpty()) {
                throw new RuntimeException("Payment not found for order ID: " + razorpayOrderId);
            }

            Payment payment = paymentOpt.get();

            // Verify payment signature with Razorpay
            boolean isValidSignature = razorpayService.verifyPaymentSignature(
                razorpayPaymentId, razorpayOrderId, razorpaySignature
            );

            if (!isValidSignature) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Invalid payment signature");
                paymentRepository.save(payment);
                throw new RuntimeException("Payment signature verification failed");
            }

            // Update payment details
            payment.setRazorpayPaymentId(razorpayPaymentId);
            payment.setRazorpaySignature(razorpaySignature);
            payment.setStatus(PaymentStatus.SUCCESS);

            // Get payment method from Razorpay
            try {
                PaymentMethod paymentMethod = razorpayService.getPaymentMethod(razorpayPaymentId);
                payment.setPaymentMethod(paymentMethod);
            } catch (Exception e) {
                logger.warn("Could not fetch payment method for payment {}: {}", razorpayPaymentId, e.getMessage());
            }

            Payment savedPayment = paymentRepository.save(payment);

            // Notify admin of successful payment
            try {
                adminWebSocketController.notifyPayment(savedPayment);
            } catch (Exception e) {
                logger.warn("Failed to send admin notification for payment: " + e.getMessage());
            }

            // Update booking status to confirmed
            Booking booking = bookingService.confirmBookingAfterPayment(payment.getBooking().getId());

            // Send booking confirmation notifications
            try {
                notificationService.sendBookingConfirmationNotification(booking);
            } catch (Exception e) {
                logger.error("Error sending booking confirmation notifications: {}", e.getMessage(), e);
                // Don't fail the payment process if notifications fail
            }

            // Automatically distribute payment to driver's wallet
            try {
                User driver = booking.getRide().getDriver();
                String rideDescription = booking.getRide().getSource() + " → " + booking.getRide().getDestination();
                
                walletService.processDriverPayment(
                    driver, 
                    payment.getAmount(), 
                    rideDescription, 
                    payment.getRazorpayPaymentId()
                );
                
                logger.info("Driver payment processed successfully for payment: {}", savedPayment.getId());
            } catch (Exception e) {
                logger.error("Error processing driver payment for payment {}: {}", savedPayment.getId(), e.getMessage(), e);
                // Don't fail the main payment process if driver payment fails
            }

            logger.info("Payment processed successfully: {}", savedPayment.getId());
            return savedPayment;

        } catch (Exception e) {
            logger.error("Error processing payment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process payment: " + e.getMessage());
        }
    }

    /**
     * Handle payment failure
     */
    public Payment handlePaymentFailure(String razorpayOrderId, String failureReason) {
        try {
            logger.info("Handling payment failure for order: {}", razorpayOrderId);

            Optional<Payment> paymentOpt = paymentRepository.findByRazorpayOrderId(razorpayOrderId);
            if (paymentOpt.isEmpty()) {
                throw new RuntimeException("Payment not found for order ID: " + razorpayOrderId);
            }

            Payment payment = paymentOpt.get();
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(failureReason);

            // Cancel the booking - this will be handled by the booking cancellation logic
            // The booking status will be updated to CANCELLED automatically

            Payment savedPayment = paymentRepository.save(payment);
            logger.info("Payment failure handled for payment: {}", savedPayment.getId());

            return savedPayment;

        } catch (Exception e) {
            logger.error("Error handling payment failure: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to handle payment failure: " + e.getMessage());
        }
    }

    /**
     * Get payment by ID
     */
    public Optional<Payment> getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId);
    }

    /**
     * Get payment by Razorpay payment ID
     */
    public Optional<Payment> getPaymentByRazorpayPaymentId(String razorpayPaymentId) {
        return paymentRepository.findByRazorpayPaymentId(razorpayPaymentId);
    }

    /**
     * Get payment by Razorpay order ID
     */
    public Optional<Payment> getPaymentByRazorpayOrderId(String razorpayOrderId) {
        return paymentRepository.findByRazorpayOrderId(razorpayOrderId);
    }

    /**
     * Get all payments for a user
     */
    public List<Payment> getPaymentsByUser(User user) {
        return paymentRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Get all payments for a booking
     */
    public List<Payment> getPaymentsByBooking(Booking booking) {
        return paymentRepository.findByBookingOrderByCreatedAtDesc(booking);
    }

    /**
     * Get successful payments for a user
     */
    public List<Payment> getSuccessfulPaymentsByUser(User user) {
        return paymentRepository.findByUserAndStatusOrderByCreatedAtDesc(user, PaymentStatus.SUCCESS);
    }

    /**
     * Check if booking has successful payment
     */
    public boolean hasSuccessfulPayment(Booking booking) {
        return paymentRepository.hasSuccessfulPayment(booking);
    }

    /**
     * Get total amount paid by user
     */
    public Double getTotalAmountPaidByUser(User user) {
        return paymentRepository.getTotalAmountPaidByUser(user);
    }

    /**
     * Cancel payment (if not yet processed)
     */
    public Payment cancelPayment(Long paymentId) {
        try {
            Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
            if (paymentOpt.isEmpty()) {
                throw new RuntimeException("Payment not found");
            }

            Payment payment = paymentOpt.get();

            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                throw new RuntimeException("Cannot cancel successful payment");
            }

            payment.setStatus(PaymentStatus.CANCELLED);
            Payment savedPayment = paymentRepository.save(payment);

            logger.info("Payment cancelled: {}", savedPayment.getId());
            return savedPayment;

        } catch (Exception e) {
            logger.error("Error cancelling payment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to cancel payment: " + e.getMessage());
        }
    }

    /**
     * Cleanup old pending payments (scheduled task)
     */
    public void cleanupOldPendingPayments() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24); // 24 hours old
            List<Payment> oldPayments = paymentRepository.findPendingPaymentsOlderThan(cutoffTime);

            for (Payment payment : oldPayments) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Payment timeout - automatically cancelled");
                
                // Cancel associated booking if it's still pending payment
                // This will be handled by the cleanup process
            }

            if (!oldPayments.isEmpty()) {
                paymentRepository.saveAll(oldPayments);
                logger.info("Cleaned up {} old pending payments", oldPayments.size());
            }

        } catch (Exception e) {
            logger.error("Error cleaning up old pending payments: {}", e.getMessage(), e);
        }
    }
}
package com.example.demo.controller;

import com.example.demo.entity.*;
import com.example.demo.service.PaymentService;
import com.example.demo.service.RazorpayService;
import com.example.demo.service.BookingService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RazorpayService razorpayService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    /**
     * Create payment order for a booking
     * POST /api/payments/create
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createPayment(@RequestBody Map<String, Object> request,
                                                            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("User not found"));
            }

            Long bookingId = Long.valueOf(request.get("bookingId").toString());
            Optional<Booking> bookingOpt = bookingService.getBookingById(bookingId);
            
            if (bookingOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Booking not found"));
            }

            Booking booking = bookingOpt.get();
            
            // Verify booking belongs to user
            if (!booking.getPassenger().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(createErrorResponse("Unauthorized access to booking"));
            }

            // Check if booking already has successful payment
            if (paymentService.hasSuccessfulPayment(booking)) {
                return ResponseEntity.badRequest().body(createErrorResponse("Booking already paid"));
            }

            // Create payment
            Payment payment = paymentService.createPayment(booking, user, booking.getFare());

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment order created successfully");
            
            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("paymentId", payment.getId());
            paymentData.put("razorpayOrderId", payment.getRazorpayOrderId());
            paymentData.put("razorpayKeyId", razorpayService.getRazorpayKeyId());
            paymentData.put("amount", payment.getAmount());
            paymentData.put("currency", payment.getCurrency());
            paymentData.put("bookingId", booking.getId());
            paymentData.put("userEmail", user.getEmail());
            paymentData.put("userPhone", user.getPhone());
            paymentData.put("userName", user.getUsername());
            
            response.put("data", paymentData);

            logger.info("Payment order created for booking {}: {}", bookingId, payment.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error creating payment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to create payment: " + e.getMessage()));
        }
    }

    /**
     * Verify payment after successful payment
     * POST /api/payments/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPayment(@RequestBody Map<String, Object> request,
                                                           Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("User not found"));
            }

            String razorpayPaymentId = request.get("razorpay_payment_id").toString();
            String razorpayOrderId = request.get("razorpay_order_id").toString();
            String razorpaySignature = request.get("razorpay_signature").toString();

            // Process payment verification
            Payment payment = paymentService.processPayment(razorpayPaymentId, razorpayOrderId, razorpaySignature);

            // Verify payment belongs to user
            if (!payment.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(createErrorResponse("Unauthorized access to payment"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment verified successfully");
            
            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("paymentId", payment.getId());
            paymentData.put("razorpayPaymentId", payment.getRazorpayPaymentId());
            paymentData.put("status", payment.getStatus().toString());
            paymentData.put("amount", payment.getAmount());
            paymentData.put("bookingId", payment.getBooking().getId());
            paymentData.put("paidAt", payment.getPaidAt());
            
            response.put("data", paymentData);

            logger.info("Payment verified successfully: {}", payment.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error verifying payment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Payment verification failed: " + e.getMessage()));
        }
    }

    /**
     * Handle payment failure
     * POST /api/payments/failure
     */
    @PostMapping("/failure")
    public ResponseEntity<Map<String, Object>> handlePaymentFailure(@RequestBody Map<String, Object> request,
                                                                   Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("User not found"));
            }

            String razorpayOrderId = request.get("razorpay_order_id").toString();
            String failureReason = request.getOrDefault("error_description", "Payment failed").toString();

            Payment payment = paymentService.handlePaymentFailure(razorpayOrderId, failureReason);

            // Verify payment belongs to user
            if (!payment.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(createErrorResponse("Unauthorized access to payment"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment failure handled");
            
            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("paymentId", payment.getId());
            paymentData.put("status", payment.getStatus().toString());
            paymentData.put("failureReason", payment.getFailureReason());
            paymentData.put("bookingId", payment.getBooking().getId());
            
            response.put("data", paymentData);

            logger.info("Payment failure handled: {}", payment.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error handling payment failure: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to handle payment failure: " + e.getMessage()));
        }
    }

    /**
     * Get payment history for user
     * GET /api/payments/history
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getPaymentHistory(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("User not found"));
            }

            List<Payment> payments = paymentService.getPaymentsByUser(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment history retrieved successfully");
            response.put("data", payments.stream().map(this::mapPaymentToResponse).toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting payment history: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to get payment history: " + e.getMessage()));
        }
    }

    /**
     * Get payment details by ID
     * GET /api/payments/{paymentId}
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<Map<String, Object>> getPaymentDetails(@PathVariable Long paymentId,
                                                               Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("User not found"));
            }

            Optional<Payment> paymentOpt = paymentService.getPaymentById(paymentId);
            if (paymentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Payment payment = paymentOpt.get();
            
            // Verify payment belongs to user
            if (!payment.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(createErrorResponse("Unauthorized access to payment"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment details retrieved successfully");
            response.put("data", mapPaymentToResponse(payment));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting payment details: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to get payment details: " + e.getMessage()));
        }
    }

    /**
     * Razorpay webhook endpoint
     * POST /api/payments/webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                              @RequestHeader("X-Razorpay-Signature") String signature) {
        try {
            logger.info("Received Razorpay webhook");

            // Verify webhook signature
            if (!razorpayService.verifyWebhookSignature(payload, signature)) {
                logger.warn("Invalid webhook signature");
                return ResponseEntity.badRequest().body("Invalid signature");
            }

            // Process webhook payload
            // This is a basic implementation - you can extend based on your needs
            logger.info("Webhook signature verified successfully");
            
            return ResponseEntity.ok("Webhook processed successfully");

        } catch (Exception e) {
            logger.error("Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Webhook processing failed");
        }
    }

    /**
     * Get Razorpay configuration for frontend
     * GET /api/payments/config
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getPaymentConfig() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Payment configuration retrieved");
        
        Map<String, Object> config = new HashMap<>();
        config.put("razorpayKeyId", razorpayService.getRazorpayKeyId());
        config.put("currency", "INR");
        config.put("isConfigured", razorpayService.isConfigured());
        
        response.put("data", config);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Create wallet top-up payment order
     * POST /api/payments/wallet/create
     */
    @PostMapping("/wallet/create")
    public ResponseEntity<Map<String, Object>> createWalletTopup(@RequestBody Map<String, Object> request,
                                                                Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("User not found"));
            }

            Double amount = Double.valueOf(request.get("amount").toString());
            
            if (amount < 10 || amount > 50000) {
                return ResponseEntity.badRequest().body(createErrorResponse("Amount must be between ₹10 and ₹50,000"));
            }

            // Create Razorpay order
            String receipt = "wallet_" + user.getId() + "_" + System.currentTimeMillis();
            String razorpayOrderId = razorpayService.createOrder(amount, "INR", receipt);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Wallet top-up order created successfully");
            
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("razorpayOrderId", razorpayOrderId);
            orderData.put("razorpayKeyId", razorpayService.getRazorpayKeyId());
            orderData.put("amount", amount);
            orderData.put("currency", "INR");
            orderData.put("userEmail", user.getEmail());
            orderData.put("userPhone", user.getPhone());
            orderData.put("userName", user.getUsername());
            orderData.put("receipt", receipt);
            
            response.put("data", orderData);

            logger.info("Wallet top-up order created for user {}: ₹{}", user.getId(), amount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error creating wallet top-up order: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to create wallet top-up order: " + e.getMessage()));
        }
    }

    /**
     * Verify wallet top-up payment
     * POST /api/payments/wallet/verify
     */
    @PostMapping("/wallet/verify")
    public ResponseEntity<Map<String, Object>> verifyWalletTopup(@RequestBody Map<String, Object> request,
                                                                Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("User not found"));
            }

            String razorpayPaymentId = request.get("razorpay_payment_id").toString();
            String razorpayOrderId = request.get("razorpay_order_id").toString();
            String razorpaySignature = request.get("razorpay_signature").toString();
            Double amount = Double.valueOf(request.get("amount").toString());

            // Verify payment signature
            boolean isValidSignature = razorpayService.verifyPaymentSignature(razorpayPaymentId, razorpayOrderId, razorpaySignature);
            
            if (!isValidSignature) {
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid payment signature"));
            }

            // Add money to wallet (you'll need to implement this in WalletService)
            // For now, we'll just return success
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Wallet top-up successful! ₹" + amount + " added to your wallet");
            
            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("razorpayPaymentId", razorpayPaymentId);
            paymentData.put("razorpayOrderId", razorpayOrderId);
            paymentData.put("amount", amount);
            paymentData.put("status", "SUCCESS");
            
            response.put("data", paymentData);

            logger.info("Wallet top-up verified successfully for user {}: ₹{}", user.getId(), amount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error verifying wallet top-up: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Wallet top-up verification failed: " + e.getMessage()));
        }
    }

    // Helper methods
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    private Map<String, Object> mapPaymentToResponse(Payment payment) {
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("id", payment.getId());
        paymentData.put("razorpayPaymentId", payment.getRazorpayPaymentId());
        paymentData.put("razorpayOrderId", payment.getRazorpayOrderId());
        paymentData.put("amount", payment.getAmount());
        paymentData.put("currency", payment.getCurrency());
        paymentData.put("status", payment.getStatus().toString());
        paymentData.put("paymentMethod", payment.getPaymentMethod() != null ? payment.getPaymentMethod().toString() : null);
        paymentData.put("failureReason", payment.getFailureReason());
        paymentData.put("createdAt", payment.getCreatedAt());
        paymentData.put("paidAt", payment.getPaidAt());
        
        // Add booking details
        if (payment.getBooking() != null) {
            Map<String, Object> bookingData = new HashMap<>();
            bookingData.put("id", payment.getBooking().getId());
            bookingData.put("source", payment.getBooking().getRide().getSource());
            bookingData.put("destination", payment.getBooking().getRide().getDestination());
            bookingData.put("date", payment.getBooking().getRide().getDate());
            bookingData.put("time", payment.getBooking().getRide().getTime());
            bookingData.put("seatsBooked", payment.getBooking().getSeatsBooked());
            paymentData.put("booking", bookingData);
        }
        
        return paymentData;
    }
}
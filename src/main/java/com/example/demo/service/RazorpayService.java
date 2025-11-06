package com.example.demo.service;

import com.example.demo.entity.PaymentMethod;
import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

@Service
public class RazorpayService {

    private static final Logger logger = LoggerFactory.getLogger(RazorpayService.class);

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @Value("${payment.test.mode:false}")
    private boolean testMode;

    private RazorpayClient razorpayClient;

    /**
     * Initialize Razorpay client
     */
    private RazorpayClient getRazorpayClient() throws RazorpayException {
        if (razorpayClient == null) {
            if (isConfigured()) {
                razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            } else {
                throw new RazorpayException("Razorpay credentials not configured");
            }
        }
        return razorpayClient;
    }

    /**
     * Check if Razorpay is configured
     */
    public boolean isConfigured() {
        return razorpayKeyId != null && !razorpayKeyId.isEmpty() && 
               !"YOUR_RAZORPAY_KEY_ID_HERE".equals(razorpayKeyId) &&
               razorpayKeySecret != null && !razorpayKeySecret.isEmpty() &&
               !"YOUR_RAZORPAY_KEY_SECRET_HERE".equals(razorpayKeySecret);
    }

    /**
     * Create a Razorpay order
     */
    public String createOrder(Double amount, String currency, String receipt) throws RazorpayException {
        try {
            logger.info("Creating Razorpay order for amount: ₹{}", amount);

            // Test mode - return mock order ID
            if (testMode) {
                String mockOrderId = "order_test_" + System.currentTimeMillis();
                logger.info("Test mode: Mock order created: {}", mockOrderId);
                return mockOrderId;
            }

            if (!isConfigured()) {
                throw new RazorpayException("Razorpay not configured. Please add valid API keys.");
            }

            RazorpayClient client = getRazorpayClient();

            // Convert amount to paise (Razorpay expects amount in smallest currency unit)
            int amountInPaise = (int) (amount * 100);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", receipt);

            Order order = client.orders.create(orderRequest);
            String orderId = order.get("id");

            logger.info("Razorpay order created successfully: {}", orderId);
            return orderId;

        } catch (RazorpayException e) {
            logger.error("Error creating Razorpay order: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error creating Razorpay order: {}", e.getMessage(), e);
            throw new RazorpayException("Failed to create order: " + e.getMessage());
        }
    }

    /**
     * Verify payment signature
     */
    public boolean verifyPaymentSignature(String razorpayPaymentId, String razorpayOrderId, String razorpaySignature) {
        try {
            logger.info("Verifying payment signature for payment: {}", razorpayPaymentId);

            // Test mode - always return true for test payments
            if (testMode) {
                logger.info("Test mode: Payment signature verification bypassed");
                return true;
            }

            if (!isConfigured()) {
                logger.error("Razorpay not configured for signature verification");
                return false;
            }

            // Create signature verification payload
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            
            // Generate expected signature
            String expectedSignature = generateSignature(payload, razorpayKeySecret);

            boolean isValid = expectedSignature.equals(razorpaySignature);
            
            if (isValid) {
                logger.info("Payment signature verified successfully");
            } else {
                logger.warn("Payment signature verification failed");
            }

            return isValid;

        } catch (Exception e) {
            logger.error("Error verifying payment signature: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get payment details from Razorpay
     */
    public Payment getPaymentDetails(String razorpayPaymentId) throws RazorpayException {
        try {
            logger.info("Fetching payment details for: {}", razorpayPaymentId);

            RazorpayClient client = getRazorpayClient();
            Payment payment = client.payments.fetch(razorpayPaymentId);

            logger.info("Payment details fetched successfully");
            return payment;

        } catch (RazorpayException e) {
            logger.error("Error fetching payment details: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get payment method from Razorpay payment
     */
    public PaymentMethod getPaymentMethod(String razorpayPaymentId) throws RazorpayException {
        try {
            Payment payment = getPaymentDetails(razorpayPaymentId);
            String method = payment.get("method");

            return mapRazorpayMethodToPaymentMethod(method);

        } catch (Exception e) {
            logger.error("Error getting payment method: {}", e.getMessage(), e);
            throw new RazorpayException("Failed to get payment method: " + e.getMessage());
        }
    }

    /**
     * Map Razorpay payment method to our PaymentMethod enum
     */
    private PaymentMethod mapRazorpayMethodToPaymentMethod(String razorpayMethod) {
        if (razorpayMethod == null) {
            return null;
        }

        switch (razorpayMethod.toLowerCase()) {
            case "card":
                return PaymentMethod.CARD;
            case "netbanking":
                return PaymentMethod.NET_BANKING;
            case "upi":
                return PaymentMethod.UPI;
            case "wallet":
                return PaymentMethod.WALLET;
            case "emi":
                return PaymentMethod.EMI;
            case "bank_transfer":
                return PaymentMethod.BANK_TRANSFER;
            default:
                logger.warn("Unknown Razorpay payment method: {}", razorpayMethod);
                return PaymentMethod.CARD; // Default fallback
        }
    }

    /**
     * Verify webhook signature
     */
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            if (!isConfigured() || webhookSecret == null || webhookSecret.isEmpty()) {
                logger.error("Webhook secret not configured");
                return false;
            }

            String expectedSignature = generateSignature(payload, webhookSecret);
            return expectedSignature.equals(signature);

        } catch (Exception e) {
            logger.error("Error verifying webhook signature: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Generate HMAC SHA256 signature
     */
    private String generateSignature(String payload, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        mac.init(secretKeySpec);
        
        byte[] hash = mac.doFinal(payload.getBytes());
        
        // Convert to hex string
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String signature = formatter.toString();
        formatter.close();
        
        return signature;
    }

    /**
     * Create refund for a payment
     */
    public String createRefund(String razorpayPaymentId, Double amount, String reason) throws RazorpayException {
        try {
            logger.info("Creating refund for payment: {} amount: ₹{}", razorpayPaymentId, amount);

            RazorpayClient client = getRazorpayClient();

            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", (int) (amount * 100)); // Convert to paise
            if (reason != null && !reason.isEmpty()) {
                refundRequest.put("notes", new JSONObject().put("reason", reason));
            }

            Payment payment = client.payments.fetch(razorpayPaymentId);
            com.razorpay.Refund refund = client.payments.refund(razorpayPaymentId, refundRequest);
            
            String refundId = refund.get("id");
            logger.info("Refund created successfully: {}", refundId);
            
            return refundId;

        } catch (RazorpayException e) {
            logger.error("Error creating refund: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get Razorpay key ID for frontend
     */
    public String getRazorpayKeyId() {
        return testMode ? "rzp_test_demo_key" : razorpayKeyId;
    }

    /**
     * Check if running in test mode
     */
    public boolean isTestMode() {
        return testMode;
    }
}
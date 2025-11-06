package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.entity.Ride;
import com.example.demo.entity.User;
import com.example.demo.entity.Payment;
import com.example.demo.entity.PaymentStatus;
import com.example.demo.repository.RideRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
public class AdminWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // Send real-time updates every 30 seconds
    @Scheduled(fixedRate = 30000)
    public void sendAdminUpdates() {
        try {
            Map<String, Object> updates = new HashMap<>();
            
            // Get current statistics
            long totalUsers = userRepository.count();
            long totalRides = rideRepository.count();
            long activeRides = rideRepository.countByStatus("ACTIVE");
            Double totalRevenue = paymentRepository.sumByStatus(PaymentStatus.SUCCESS);
            if (totalRevenue == null) totalRevenue = 0.0;
            
            updates.put("totalUsers", totalUsers);
            updates.put("totalRides", totalRides);
            updates.put("activeRides", activeRides);
            updates.put("totalRevenue", totalRevenue);
            updates.put("timestamp", LocalDateTime.now());
            
            // Send to admin dashboard
            messagingTemplate.convertAndSend("/topic/admin/stats", updates);
            
        } catch (Exception e) {
            System.err.println("Error sending admin updates: " + e.getMessage());
        }
    }

    // Handle new user registration notifications
    public void notifyNewUser(User user) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "NEW_USER");
            notification.put("message", "New " + user.getRole().toString().toLowerCase() + " registered: " + user.getName());
            notification.put("user", Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole()
            ));
            notification.put("timestamp", LocalDateTime.now());
            
            messagingTemplate.convertAndSend("/topic/admin/notifications", notification);
        } catch (Exception e) {
            System.err.println("Error sending new user notification: " + e.getMessage());
        }
    }

    // Handle new ride creation notifications
    public void notifyNewRide(Ride ride) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "NEW_RIDE");
            notification.put("message", "New ride created: " + ride.getSource() + " → " + ride.getDestination());
            notification.put("ride", Map.of(
                "id", ride.getId(),
                "source", ride.getSource(),
                "destination", ride.getDestination(),
                "driver", ride.getDriver().getName(),
                "date", ride.getDate(),
                "time", ride.getTime(),
                "fare", ride.getFare()
            ));
            notification.put("timestamp", LocalDateTime.now());
            
            messagingTemplate.convertAndSend("/topic/admin/notifications", notification);
        } catch (Exception e) {
            System.err.println("Error sending new ride notification: " + e.getMessage());
        }
    }

    // Handle payment notifications
    public void notifyPayment(Payment payment) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "PAYMENT");
            notification.put("message", "Payment " + payment.getStatus() + ": Rs." + payment.getAmount());
            notification.put("payment", Map.of(
                "id", payment.getId(),
                "amount", payment.getAmount(),
                "status", payment.getStatus(),
                "user", payment.getUser().getName()
            ));
            notification.put("timestamp", LocalDateTime.now());
            
            messagingTemplate.convertAndSend("/topic/admin/notifications", notification);
        } catch (Exception e) {
            System.err.println("Error sending payment notification: " + e.getMessage());
        }
    }

    @MessageMapping("/admin/subscribe")
    @SendTo("/topic/admin/stats")
    public Map<String, Object> subscribeToAdminUpdates() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Subscribed to admin updates");
        response.put("timestamp", LocalDateTime.now());
        return response;
    }
}
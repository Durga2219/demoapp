package com.example.demo.controller;

import com.example.demo.dto.NotificationResponse;
import com.example.demo.entity.User;
import com.example.demo.service.NotificationService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class WebSocketNotificationController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    // Handle user connection and send initial notifications
    @MessageMapping("/notifications.connect")
    public void connectUser(@Payload String username, Principal principal) {
        try {
            User user = userService.findByUsername(principal.getName());
            
            // Send initial notifications to the connected user
            List<NotificationResponse> notifications = notificationService.getUserNotifications(user)
                .stream()
                .map(NotificationResponse::new)
                .collect(Collectors.toList());

            // Send notifications to the specific user
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/notifications",
                notifications
            );

            // Send unread count
            Long unreadCount = notificationService.getUnreadCount(user);
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/unread-count",
                unreadCount
            );

        } catch (Exception e) {
            System.err.println("Error handling user connection: " + e.getMessage());
        }
    }

    // Send notification to specific user
    public void sendNotificationToUser(String username, NotificationResponse notification) {
        messagingTemplate.convertAndSendToUser(
            username,
            "/queue/notifications",
            notification
        );
    }

    // Send unread count update to specific user
    public void sendUnreadCountToUser(String username, Long unreadCount) {
        messagingTemplate.convertAndSendToUser(
            username,
            "/queue/unread-count",
            unreadCount
        );
    }

    // Broadcast notification to all users (for system-wide announcements)
    public void broadcastNotification(NotificationResponse notification) {
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }

    // Send ride status update to specific users (driver and passengers)
    public void sendRideStatusUpdate(String username, Map<String, Object> rideStatusUpdate) {
        messagingTemplate.convertAndSendToUser(
            username,
            "/queue/ride-status-updates",
            rideStatusUpdate
        );
    }
}
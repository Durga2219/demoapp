package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.NotificationResponse;
import com.example.demo.entity.User;
import com.example.demo.service.NotificationService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    // Get user notifications
    @GetMapping
    public ResponseEntity<ApiResponse> getUserNotifications(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            List<NotificationResponse> notifications = notificationService.getUserNotifications(user)
                .stream()
                .map(NotificationResponse::new)
                .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse(true, "Notifications retrieved successfully", notifications));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Failed to retrieve notifications: " + e.getMessage(), null));
        }
    }

    // Get unread notifications count
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse> getUnreadCount(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Long unreadCount = notificationService.getUnreadCount(user);

            return ResponseEntity.ok(new ApiResponse(true, "Unread count retrieved successfully", unreadCount));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Failed to retrieve unread count: " + e.getMessage(), null));
        }
    }

    // Mark notification as read
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse> markAsRead(@PathVariable Long notificationId) {
        try {
            notificationService.markAsRead(notificationId);
            return ResponseEntity.ok(new ApiResponse(true, "Notification marked as read", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Failed to mark notification as read: " + e.getMessage(), null));
        }
    }

    // Mark all notifications as read
    @PutMapping("/mark-all-read")
    public ResponseEntity<ApiResponse> markAllAsRead(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            notificationService.markAllAsRead(user);
            return ResponseEntity.ok(new ApiResponse(true, "All notifications marked as read", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Failed to mark all notifications as read: " + e.getMessage(), null));
        }
    }
}
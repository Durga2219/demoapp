package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.entity.NotificationType;
import com.example.demo.entity.User;
import com.example.demo.service.NotificationService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @PostMapping("/notification")
    public ResponseEntity<ApiResponse> testNotification(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            
            // Create a test notification
            notificationService.createNotification(
                user,
                "Test Notification 🔔",
                "This is a test notification to verify WebSocket functionality is working properly!",
                NotificationType.GENERAL
            );

            return ResponseEntity.ok(new ApiResponse(true, "Test notification sent successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Failed to send test notification: " + e.getMessage(), null));
        }
    }
}
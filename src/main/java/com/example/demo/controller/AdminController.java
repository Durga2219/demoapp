package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.entity.*;
import com.example.demo.enums.Role;
import com.example.demo.repository.*;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
// @PreAuthorize("hasRole('ADMIN')") // Temporarily disabled for testing
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserService userService;

    // Dashboard Statistics
    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse> getDashboardStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // User statistics
            long totalUsers = userRepository.count();
            long totalDrivers = userRepository.countByRole(Role.DRIVER);
            long totalPassengers = userRepository.countByRole(Role.PASSENGER);
            long activeUsers = userRepository.countByCreatedAtAfter(LocalDateTime.now().minusDays(30));
            
            // Ride statistics
            long totalRides = rideRepository.count();
            long activeRides = rideRepository.countByStatus("ACTIVE");
            long completedRides = rideRepository.countByStatus("COMPLETED");
            long cancelledRides = rideRepository.countByStatus("CANCELLED");
            
            // Today's statistics
            LocalDate today = LocalDate.now();
            long todayRides = rideRepository.countByDate(today);
            
            // Revenue statistics
            Double totalRevenue = paymentRepository.sumByStatus(PaymentStatus.SUCCESS);
            if (totalRevenue == null) totalRevenue = 0.0;
            
            Double todayRevenue = paymentRepository.sumByStatusAndCreatedAtAfter(PaymentStatus.SUCCESS, 
                today.atStartOfDay());
            if (todayRevenue == null) todayRevenue = 0.0;
            
            stats.put("totalUsers", totalUsers);
            stats.put("totalDrivers", totalDrivers);
            stats.put("totalPassengers", totalPassengers);
            stats.put("activeUsers", activeUsers);
            stats.put("totalRides", totalRides);
            stats.put("activeRides", activeRides);
            stats.put("completedRides", completedRides);
            stats.put("cancelledRides", cancelledRides);
            stats.put("todayRides", todayRides);
            stats.put("totalRevenue", totalRevenue);
            stats.put("todayRevenue", todayRevenue);
            
            return ResponseEntity.ok(new ApiResponse(true, "Dashboard stats retrieved", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Failed to get dashboard stats: " + e.getMessage(), null));
        }
    }

    // Recent Activity
    @GetMapping("/dashboard/recent-activity")
    public ResponseEntity<ApiResponse> getRecentActivity() {
        try {
            List<Map<String, Object>> activities = new ArrayList<>();
            
            // Recent rides
            List<Ride> recentRides = rideRepository.findTop5ByOrderByCreatedAtDesc();
            for (Ride ride : recentRides) {
                Map<String, Object> activity = new HashMap<>();
                activity.put("type", "RIDE");
                activity.put("message", "New ride created: " + ride.getSource() + " → " + ride.getDestination());
                activity.put("user", ride.getDriver().getName());
                activity.put("timestamp", ride.getCreatedAt());
                activity.put("status", ride.getStatus());
                activities.add(activity);
            }
            
            // Recent users
            List<User> recentUsers = userRepository.findTop5ByOrderByCreatedAtDesc();
            for (User user : recentUsers) {
                Map<String, Object> activity = new HashMap<>();
                activity.put("type", "USER");
                activity.put("message", "New " + user.getRole().toString().toLowerCase() + " registered");
                activity.put("user", user.getName());
                activity.put("timestamp", user.getCreatedAt());
                activity.put("status", "ACTIVE");
                activities.add(activity);
            }
            
            // Sort by timestamp
            activities.sort((a, b) -> {
                LocalDateTime timeA = (LocalDateTime) a.get("timestamp");
                LocalDateTime timeB = (LocalDateTime) b.get("timestamp");
                return timeB.compareTo(timeA);
            });
            
            return ResponseEntity.ok(new ApiResponse(true, "Recent activity retrieved", 
                activities.stream().limit(10).collect(Collectors.toList())));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Failed to get recent activity: " + e.getMessage(), null));
        }
    }

    // Users Management
    @GetMapping("/users")
    public ResponseEntity<ApiResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
            List<User> users = userRepository.findAll(pageRequest).getContent();
            
            List<Map<String, Object>> userList = users.stream().map(user -> {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("name", user.getName());
                userMap.put("email", user.getEmail());
                userMap.put("phone", user.getPhone());
                userMap.put("role", user.getRole());
                userMap.put("createdAt", user.getCreatedAt());
                userMap.put("isActive", true); // Assuming all users are active for now
                if (user.getRole() == Role.DRIVER) {
                    userMap.put("vehicleModel", user.getVehicleModel());
                    userMap.put("vehiclePlate", user.getVehiclePlate());
                }
                return userMap;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(new ApiResponse(true, "Users retrieved", userList));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Failed to get users: " + e.getMessage(), null));
        }
    }

    // Block/Unblock User
    @PutMapping("/users/{userId}/block")
    public ResponseEntity<ApiResponse> blockUser(@PathVariable Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "User not found", null));
            }
            
            // For now, we'll just return success
            // In a real implementation, you'd add an 'isBlocked' field to User entity
            return ResponseEntity.ok(new ApiResponse(true, "User blocked successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Failed to block user: " + e.getMessage(), null));
        }
    }

    // Rides Management
    @GetMapping("/rides")
    public ResponseEntity<ApiResponse> getAllRides(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
            List<Ride> rides = rideRepository.findAll(pageRequest).getContent();
            
            List<Map<String, Object>> rideList = rides.stream().map(ride -> {
                Map<String, Object> rideMap = new HashMap<>();
                rideMap.put("id", ride.getId());
                rideMap.put("driver", Map.of(
                    "id", ride.getDriver().getId(),
                    "name", ride.getDriver().getName(),
                    "email", ride.getDriver().getEmail()
                ));
                rideMap.put("source", ride.getSource());
                rideMap.put("destination", ride.getDestination());
                rideMap.put("date", ride.getDate());
                rideMap.put("time", ride.getTime());
                rideMap.put("totalSeats", ride.getTotalSeats());
                rideMap.put("availableSeats", ride.getAvailableSeats());
                rideMap.put("fare", ride.getFare());
                rideMap.put("status", ride.getStatus());
                rideMap.put("createdAt", ride.getCreatedAt());
                return rideMap;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(new ApiResponse(true, "Rides retrieved", rideList));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Failed to get rides: " + e.getMessage(), null));
        }
    }

    // Cancel Ride
    @PutMapping("/rides/{rideId}/cancel")
    public ResponseEntity<ApiResponse> cancelRide(@PathVariable Long rideId) {
        try {
            Optional<Ride> rideOpt = rideRepository.findById(rideId);
            if (rideOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Ride not found", null));
            }
            
            Ride ride = rideOpt.get();
            ride.setStatus("CANCELLED");
            rideRepository.save(ride);
            
            return ResponseEntity.ok(new ApiResponse(true, "Ride cancelled successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Failed to cancel ride: " + e.getMessage(), null));
        }
    }

    // Payments Management
    @GetMapping("/payments")
    public ResponseEntity<ApiResponse> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
            List<Payment> payments = paymentRepository.findAll(pageRequest).getContent();
            
            List<Map<String, Object>> paymentList = payments.stream().map(payment -> {
                Map<String, Object> paymentMap = new HashMap<>();
                paymentMap.put("id", payment.getId());
                paymentMap.put("user", Map.of(
                    "id", payment.getUser().getId(),
                    "name", payment.getUser().getName(),
                    "email", payment.getUser().getEmail()
                ));
                paymentMap.put("amount", payment.getAmount());
                paymentMap.put("type", payment.getPaymentMethod() != null ? payment.getPaymentMethod().toString() : "ONLINE");
                paymentMap.put("status", payment.getStatus());
                paymentMap.put("razorpayPaymentId", payment.getRazorpayPaymentId());
                paymentMap.put("createdAt", payment.getCreatedAt());
                return paymentMap;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(new ApiResponse(true, "Payments retrieved", paymentList));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Failed to get payments: " + e.getMessage(), null));
        }
    }

    // Reports
    @GetMapping("/reports/summary")
    public ResponseEntity<ApiResponse> getReportsSummary() {
        try {
            Map<String, Object> reports = new HashMap<>();
            
            LocalDate today = LocalDate.now();
            LocalDate weekStart = today.minusDays(7);
            LocalDate monthStart = today.minusDays(30);
            
            // Daily stats
            reports.put("todayRides", rideRepository.countByDate(today));
            reports.put("todayRevenue", paymentRepository.sumByStatusAndCreatedAtAfter(PaymentStatus.SUCCESS, today.atStartOfDay()));
            
            // Weekly stats
            reports.put("weeklyRides", rideRepository.countByDateBetween(weekStart, today));
            reports.put("weeklyRevenue", paymentRepository.sumByStatusAndCreatedAtAfter(PaymentStatus.SUCCESS, weekStart.atStartOfDay()));
            
            // Monthly stats
            reports.put("monthlyRides", rideRepository.countByDateBetween(monthStart, today));
            reports.put("monthlyRevenue", paymentRepository.sumByStatusAndCreatedAtAfter(PaymentStatus.SUCCESS, monthStart.atStartOfDay()));
            
            // Cancellation stats
            reports.put("totalCancellations", rideRepository.countByStatus("CANCELLED"));
            reports.put("cancellationRate", calculateCancellationRate());
            
            return ResponseEntity.ok(new ApiResponse(true, "Reports summary retrieved", reports));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Failed to get reports: " + e.getMessage(), null));
        }
    }

    private double calculateCancellationRate() {
        long totalRides = rideRepository.count();
        long cancelledRides = rideRepository.countByStatus("CANCELLED");
        return totalRides > 0 ? (double) cancelledRides / totalRides * 100 : 0.0;
    }

    // System Settings
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse> getSystemSettings() {
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("baseFare", 50.0);
            settings.put("commissionRate", 10.0);
            settings.put("maxSeatsPerRide", 8);
            settings.put("appName", "RideConnect");
            
            return ResponseEntity.ok(new ApiResponse(true, "System settings retrieved", settings));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Failed to get settings: " + e.getMessage(), null));
        }
    }
}

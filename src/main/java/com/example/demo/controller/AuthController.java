package com.example.demo.controller;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.ApiResponse;
import com.example.demo.service.AuthService;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthService authService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ==================== TEST USER CREATION ====================
    @PostMapping("/create-test-user")
    public ResponseEntity<ApiResponse> createTestUser() {
        try {
            RegisterRequest testUser = new RegisterRequest();
            testUser.setUsername("testuser");
            testUser.setEmail("test@test.com");
            testUser.setPhone("1234567890");
            testUser.setPassword("123456");
            testUser.setRole("PASSENGER");
            
            AuthResponse response = authService.register(testUser);
            log.info("Test user created successfully");
            return ResponseEntity.ok(new ApiResponse(true, "Test user created", response));
        } catch (Exception e) {
            log.error("Failed to create test user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Test user already exists or creation failed", null));
        }
    }

    // ==================== REGISTER ====================
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> registerUser(@Valid @RequestBody RegisterRequest request,
                                                    BindingResult result) {
        if (result.hasErrors()) {
            String errorMsg = result.getFieldError() != null
                    ? result.getFieldError().getDefaultMessage()
                    : "Invalid registration data";
            log.warn("Registration validation failed - {}", errorMsg);
            return ResponseEntity.badRequest().body(new ApiResponse(false, errorMsg, null));
        }

        try {
            AuthResponse response = authService.register(request);
            log.info("Registration successful - username: {}", request.getUsername());
            return ResponseEntity.ok(new ApiResponse(true, "Registration successful", response));

        } catch (Exception e) {
            log.error("Registration failed - username: {}, error: {}",
                    request.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ==================== LOGIN ====================
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request,
                                             BindingResult result) {
        if (result.hasErrors()) {
            String errorMsg = result.getFieldError() != null
                    ? result.getFieldError().getDefaultMessage()
                    : "Invalid login data";
            log.warn("Login validation failed - {}", errorMsg);
            return ResponseEntity.badRequest().body(new ApiResponse(false, errorMsg, null));
        }

        try {
            AuthResponse response = authService.login(request);
            
            // Add role-based redirect URL for professional access
            User user = userRepository.findByEmail(request.getUsername())
                    .orElse(userRepository.findByUsername(request.getUsername()).orElse(null));
            
            if (user != null) {
                // Check if user is blocked
                if (user.getBlocked() != null && user.getBlocked()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(new ApiResponse(false, "Account is blocked: " + user.getBlockReason(), null));
                }
                
                String redirectUrl = switch (user.getRole()) {
                    case ADMIN -> "/admin-dashboard.html";
                    case DRIVER -> "/driver-dashboard.html";
                    case BOTH -> "/driver-dashboard.html";
                    default -> "/passenger-dashboard.html";
                };
                response.setRedirectUrl(redirectUrl);
                response.setRole(user.getRole());
            }
            
            log.info("Login successful - identifier: {}, role: {}", request.getUsername(), 
                    user != null ? user.getRole() : "UNKNOWN");
            return ResponseEntity.ok(new ApiResponse(true, "Login successful", response));

        } catch (Exception e) {
            log.error("Login failed - identifier: {}, error: {}",
                    request.getUsername(), e.getMessage());
            // Generic error to prevent exposing sensitive info
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Invalid username or password", null));
        }
    }

    // ==================== PROFESSIONAL ADMIN ACCESS ====================
    @PostMapping("/admin-access")
    public ResponseEntity<ApiResponse> adminAccess(@Valid @RequestBody LoginRequest request,
                                                   BindingResult result) {
        if (result.hasErrors()) {
            String errorMsg = result.getFieldError() != null
                    ? result.getFieldError().getDefaultMessage()
                    : "Invalid login data";
            return ResponseEntity.badRequest().body(new ApiResponse(false, errorMsg, null));
        }

        try {
            // First authenticate the user
            AuthResponse response = authService.login(request);
            
            // Check if user has admin role
            User user = userRepository.findByEmail(request.getUsername())
                    .orElse(userRepository.findByUsername(request.getUsername()).orElse(null));
            
            if (user == null || !user.getRole().equals(Role.ADMIN)) {
                log.warn("Non-admin user attempted admin access: {}", request.getUsername());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "Admin access denied. Insufficient privileges.", null));
            }
            
            // Check if admin account is blocked
            if (user.getBlocked() != null && user.getBlocked()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "Admin account is blocked: " + user.getBlockReason(), null));
            }
            
            response.setRedirectUrl("/admin-dashboard.html");
            response.setRole(Role.ADMIN);
            
            log.info("Admin access granted - user: {}", request.getUsername());
            return ResponseEntity.ok(new ApiResponse(true, "Admin access granted", response));

        } catch (Exception e) {
            log.error("Admin access failed - identifier: {}, error: {}",
                    request.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Invalid credentials", null));
        }
    }

    // ==================== CREATE ADMIN USER ====================
    @PostMapping("/create-admin")
    public ResponseEntity<ApiResponse> createAdmin(@RequestBody Map<String, String> adminData) {
        try {
            // Check if admin already exists
            if (userRepository.existsByRole(Role.ADMIN)) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Admin user already exists", null));
            }
            
            RegisterRequest adminRequest = new RegisterRequest();
            adminRequest.setUsername(adminData.getOrDefault("username", "admin"));
            adminRequest.setEmail(adminData.getOrDefault("email", "admin@rideconnect.com"));
            adminRequest.setPhone(adminData.getOrDefault("phone", "9999999999"));
            adminRequest.setPassword(adminData.getOrDefault("password", "admin123"));
            adminRequest.setRole("ADMIN");
            
            AuthResponse response = authService.register(adminRequest);
            log.info("Admin user created successfully");
            return ResponseEntity.ok(new ApiResponse(true, "Admin user created successfully", response));
            
        } catch (Exception e) {
            log.error("Failed to create admin user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Failed to create admin user: " + e.getMessage(), null));
        }
    }

    // ==================== OTP AUTHENTICATION ====================
    
    @PostMapping("/send-registration-otp")
    public ResponseEntity<ApiResponse> sendRegistrationOtp(@Valid @RequestBody com.example.demo.dto.OtpRequest request, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Validation failed: " + result.getAllErrors().get(0).getDefaultMessage(), null));
        }

        try {
            authService.sendRegistrationOtp(request.getEmail());
            return ResponseEntity.ok(new ApiResponse(true, "OTP sent to your email successfully", null));
        } catch (Exception e) {
            log.error("Error sending registration OTP", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    @PostMapping("/send-login-otp")
    public ResponseEntity<ApiResponse> sendLoginOtp(@Valid @RequestBody com.example.demo.dto.OtpRequest request, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Validation failed: " + result.getAllErrors().get(0).getDefaultMessage(), null));
        }

        try {
            authService.sendLoginOtp(request.getEmail());
            return ResponseEntity.ok(new ApiResponse(true, "OTP sent to your email successfully", null));
        } catch (Exception e) {
            log.error("Error sending login OTP", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    @PostMapping("/register-with-otp")
    public ResponseEntity<ApiResponse> registerWithOtp(@Valid @RequestBody com.example.demo.dto.RegisterWithOtpRequest request, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Validation failed: " + result.getAllErrors().get(0).getDefaultMessage(), null));
        }

        try {
            AuthResponse authResponse = authService.registerWithOtp(request.toRegisterRequest(), request.getOtp());
            return ResponseEntity.ok(new ApiResponse(true, "Registration successful", authResponse));
        } catch (Exception e) {
            log.error("Error registering with OTP", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    @PostMapping("/login-with-otp")
    public ResponseEntity<ApiResponse> loginWithOtp(@Valid @RequestBody com.example.demo.dto.OtpVerificationRequest request, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Validation failed: " + result.getAllErrors().get(0).getDefaultMessage(), null));
        }

        try {
            AuthResponse authResponse = authService.loginWithOtp(request.getEmail(), request.getOtp());
            return ResponseEntity.ok(new ApiResponse(true, "Login successful", authResponse));
        } catch (Exception e) {
            log.error("Error logging in with OTP", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ==================== UPDATE PROFILE PICTURE ====================
    @PutMapping("/update-profile-picture")
    public ResponseEntity<ApiResponse> updateProfilePicture(@RequestBody Map<String, String> request,
                                                            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            String profilePicture = request.get("profilePicture");
            
            if (profilePicture == null || profilePicture.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Profile picture data is required", null));
            }
            
            // Find user by email or username
            User user = userRepository.findByEmail(userEmail)
                    .orElse(userRepository.findByUsername(userEmail)
                            .orElseThrow(() -> new RuntimeException("User not found")));
            
            // Update profile picture
            user.setProfilePicture(profilePicture);
            userRepository.save(user);
            
            log.info("Profile picture updated for user: {}", userEmail);
            return ResponseEntity.ok(new ApiResponse(true, "Profile picture updated successfully", null));
            
        } catch (Exception e) {
            log.error("Error updating profile picture: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to update profile picture", null));
        }
    }

    // ==================== GET PROFILE ====================
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse> getProfile(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            
            // Find user by email or username
            User user = userRepository.findByEmail(userEmail)
                    .orElseGet(() -> userRepository.findByUsername(userEmail)
                            .orElseThrow(() -> new RuntimeException("User not found")));
            
            log.info("Profile retrieved for user: {}", userEmail);
            return ResponseEntity.ok(new ApiResponse(true, "Profile retrieved successfully", user));
            
        } catch (Exception e) {
            log.error("Error retrieving profile: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to retrieve profile", null));
        }
    }

    // ==================== TEST ====================
    @GetMapping("/test")
    public ResponseEntity<ApiResponse> test() {
        return ResponseEntity.ok(new ApiResponse(true, "Auth API working!", null));
    }

    // ==================== VERIFY TOKEN ====================
    @GetMapping("/verify")
    public ResponseEntity<ApiResponse> verifyToken(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                log.error("❌ Token verification failed: Authentication is null or not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Token is invalid or expired", null));
            }

            // 🔧 FIX: The principal is a User object, not a string
            Object principal = authentication.getPrincipal();
            User user;
            
            if (principal instanceof User) {
                // ✅ Token was validated by JWT filter, User object is in principal
                user = (User) principal;
                log.info("✅ Token verified for user ID: {}, username: {}", user.getId(), user.getUsername());
            } else {
                // 🔍 Fallback: try to get username and find user
                String username = authentication.getName();
                user = userRepository.findByEmail(username)
                    .orElse(userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("User not found with identifier: " + username)));
                log.info("✅ Token verified for user (via fallback): {}", username);
            }
            
            return ResponseEntity.ok(new ApiResponse(true, "Token is valid", user));
        } catch (Exception e) {
            log.error("❌ Token verification failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(false, "Token verification failed: " + e.getMessage(), null));
        }
    }

    @PostMapping("/test-email")
    public ResponseEntity<ApiResponse> testEmail(@RequestBody java.util.Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Email is required", null));
            }

            log.info("🧪 Testing email configuration with: {}", email);
            
            // Send a test OTP using the auth service
            authService.sendRegistrationOtp(email);
            
            return ResponseEntity.ok(new ApiResponse(true, 
                "✅ Email sent! Check your inbox (and spam folder). If in test mode, check server logs.", 
                "Email: " + email));
                
        } catch (Exception e) {
            log.error("❌ Email test failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Email test failed: " + e.getMessage(), null));
        }
    }

    @GetMapping("/email-config-status")
    public ResponseEntity<ApiResponse> getEmailConfigStatus() {
        try {
            // This will help check if email is properly configured
            java.util.Map<String, Object> status = new java.util.HashMap<>();
            status.put("testMode", "Check server logs for current mode");
            status.put("instructions", "Follow GMAIL_SETUP_STEPS.md for real email setup");
            status.put("testEndpoint", "POST /api/auth/test-email with {\"email\":\"your@email.com\"}");
            
            return ResponseEntity.ok(new ApiResponse(true, "Email configuration status", status));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Failed to get email status", null));
        }
    }

    // ==================== UPDATE PROFILE ====================
    @PutMapping("/update-profile")
    public ResponseEntity<ApiResponse> updateProfile(@RequestBody Map<String, String> request,
                                                     Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            String name = request.get("name");
            String phone = request.get("phone");
            String role = request.get("role");
            
            if (name == null || phone == null || role == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Name, phone, and role are required", null));
            }
            
            // Find user by email or username
            User user = userRepository.findByEmail(userEmail)
                    .orElse(userRepository.findByUsername(userEmail)
                            .orElseThrow(() -> new RuntimeException("User not found")));
            
            // Update profile information
            user.setName(name);
            user.setPhone(phone);
            user.setRole(Role.valueOf(role.toUpperCase()));
            userRepository.save(user);
            
            log.info("Profile updated for user: {}", userEmail);
            return ResponseEntity.ok(new ApiResponse(true, "Profile updated successfully", null));
            
        } catch (Exception e) {
            log.error("Error updating profile: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to update profile", null));
        }
    }

    // ==================== UPDATE VEHICLE INFO ====================
    @PutMapping("/update-vehicle")
    public ResponseEntity<ApiResponse> updateVehicle(@RequestBody Map<String, Object> request,
                                                     Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            String vehicleModel = (String) request.get("vehicleModel");
            String vehiclePlate = (String) request.get("vehiclePlate");
            Integer vehicleCapacity = (Integer) request.get("vehicleCapacity");
            
            if (vehicleModel == null || vehiclePlate == null || vehicleCapacity == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "All vehicle information fields are required", null));
            }
            
            // Find user by email or username
            User user = userRepository.findByEmail(userEmail)
                    .orElse(userRepository.findByUsername(userEmail)
                            .orElseThrow(() -> new RuntimeException("User not found")));
            
            // Update vehicle information
            user.setVehicleModel(vehicleModel);
            user.setVehiclePlate(vehiclePlate);
            user.setVehicleCapacity(vehicleCapacity);
            userRepository.save(user);
            
            log.info("Vehicle info updated for user: {}", userEmail);
            return ResponseEntity.ok(new ApiResponse(true, "Vehicle information updated successfully", null));
            
        } catch (Exception e) {
            log.error("Error updating vehicle info: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to update vehicle information", null));
        }
    }

    // ==================== CHANGE PASSWORD ====================
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(@RequestBody Map<String, String> request,
                                                      Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            String currentPassword = request.get("currentPassword");
            String newPassword = request.get("newPassword");
            
            if (currentPassword == null || newPassword == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Current password and new password are required", null));
            }
            
            if (newPassword.length() < 6) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "New password must be at least 6 characters long", null));
            }
            
            // Find user by email or username
            User user = userRepository.findByEmail(userEmail)
                    .orElse(userRepository.findByUsername(userEmail)
                            .orElseThrow(() -> new RuntimeException("User not found")));
            
            // Verify current password
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Current password is incorrect", null));
            }
            
            // Update password
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            
            log.info("Password changed for user: {}", userEmail);
            return ResponseEntity.ok(new ApiResponse(true, "Password changed successfully", null));
            
        } catch (Exception e) {
            log.error("Error changing password: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to change password", null));
        }
    }
}

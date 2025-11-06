package com.example.demo.service;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.entity.User;
import com.example.demo.enums.Role;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager,
                       OtpService otpService,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.otpService = otpService;
        this.emailService = emailService;
    }

    // ==================== REGISTER ====================
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setName(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // ✅ BCrypt encode
        user.setRole(determineUserRole(request.getRole()));
        user.setProfilePicture(request.getProfilePicture());
        
        // Set vehicle details for drivers
        if (request.getRole() != null && request.getRole().equalsIgnoreCase("DRIVER")) {
            user.setVehicleModel(request.getVehicleModel());
            user.setVehiclePlate(request.getVehiclePlate());
            user.setVehicleCapacity(request.getVehicleCapacity());
        }
        
        user.setEnabled(true);
        user.setAccountNonLocked(true);

        User savedUser = userRepository.save(user);

        String token = jwtUtil.generateToken(
                savedUser.getUsername(),
                savedUser.getId(),
                savedUser.getRole().name()
        );

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getName(),
                savedUser.getRole(),
                savedUser.getVehicleModel(),
                savedUser.getVehiclePlate(),
                savedUser.getVehicleCapacity(),
                savedUser.getProfilePicture()
        );
    }

    // ==================== LOGIN ====================
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository
                .findByUsernameOrEmail(request.getUsername(), request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ✅ FIXED: Proper BCrypt password check
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtUtil.generateToken(
                user.getUsername(),
                user.getId(),
                user.getRole().name()
        );

        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getVehicleModel(),
                user.getVehiclePlate(),
                user.getVehicleCapacity(),
                user.getProfilePicture()
        );
    }

    private Role determineUserRole(String roleString) {
        if (roleString == null || roleString.trim().isEmpty()) {
            return Role.PASSENGER;
        }
        try {
            return Role.valueOf(roleString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Role.PASSENGER;
        }
    }

    // ==================== OTP AUTHENTICATION ====================
    
    /**
     * Send OTP for registration
     */
    public void sendRegistrationOtp(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }
        otpService.generateAndSendOtp(email, com.example.demo.entity.OtpType.REGISTRATION);
    }

    /**
     * Send OTP for login
     */
    public void sendLoginOtp(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email not registered");
        }
        otpService.generateAndSendOtp(email, com.example.demo.entity.OtpType.LOGIN);
    }

    /**
     * Register with OTP verification
     */
    @Transactional
    public AuthResponse registerWithOtp(RegisterRequest request, String otp) {
        // Verify OTP first
        if (!otpService.verifyOtp(request.getEmail(), otp, com.example.demo.entity.OtpType.REGISTRATION)) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        // Check if email is still available
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already taken");
        }

        // Create user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setName(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(determineUserRole(request.getRole()));
        user.setProfilePicture(request.getProfilePicture());

        User savedUser = userRepository.save(user);

        // Send welcome email
        emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getName());

        // Generate JWT token
        String token = jwtUtil.generateToken(
                savedUser.getUsername(),
                savedUser.getId(),
                savedUser.getRole().name()
        );

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getName(),
                savedUser.getRole(),
                savedUser.getVehicleModel(),
                savedUser.getVehiclePlate(),
                savedUser.getVehicleCapacity(),
                savedUser.getProfilePicture()
        );
    }

    /**
     * Login with OTP verification
     */
    @Transactional(readOnly = true)
    public AuthResponse loginWithOtp(String email, String otp) {
        // Verify OTP first
        if (!otpService.verifyOtp(email, otp, com.example.demo.entity.OtpType.LOGIN)) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate JWT token
        String token = jwtUtil.generateToken(
                user.getUsername(),
                user.getId(),
                user.getRole().name()
        );

        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getVehicleModel(),
                user.getVehiclePlate(),
                user.getVehicleCapacity(),
                user.getProfilePicture()
        );
    }
}

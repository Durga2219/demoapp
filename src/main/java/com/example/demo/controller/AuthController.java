package com.example.demo.controller;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.ApiResponse;
import com.example.demo.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
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
            log.info("Login successful - identifier: {}", request.getUsername());
            return ResponseEntity.ok(new ApiResponse(true, "Login successful", response));

        } catch (Exception e) {
            log.error("Login failed - identifier: {}, error: {}",
                    request.getUsername(), e.getMessage());
            // Generic error to prevent exposing sensitive info
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Invalid username or password", null));
        }
    }

    // ==================== TEST ====================
    @GetMapping("/test")
    public ResponseEntity<ApiResponse> test() {
        return ResponseEntity.ok(new ApiResponse(true, "Auth API working!", null));
    }
}

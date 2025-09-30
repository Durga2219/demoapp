package com.ashu.ride_sharing.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ashu.ride_sharing.dto.request.LoginRequest;
import com.ashu.ride_sharing.dto.request.RefreshTokenRequest;
import com.ashu.ride_sharing.dto.request.UserRegistrationRequest;
import com.ashu.ride_sharing.dto.response.AuthResponse;
import com.ashu.ride_sharing.exception.InvalidTokenException;
import com.ashu.ride_sharing.services.AuthenticationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;



@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class UserControllers {
    
    private final AuthenticationService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(@Valid @ModelAttribute UserRegistrationRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(AuthResponse.builder().message("Invalid credentials").build());
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        try {
            String message  = authService.verifyEmail(token);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body("Invalid or expired token");
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
         try {
            return ResponseEntity.ok(authService.refreshToken(request));
        } catch (InvalidTokenException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AuthResponse.builder().message(e.getMessage()).build());
        }
    }
}


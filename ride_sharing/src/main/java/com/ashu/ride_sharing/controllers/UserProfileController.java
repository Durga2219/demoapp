package com.ashu.ride_sharing.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ashu.ride_sharing.dto.request.UpdateProfileRequest;
import com.ashu.ride_sharing.dto.response.UserDashboardResponse;
import com.ashu.ride_sharing.dto.response.UserProfileResponse;
import com.ashu.ride_sharing.services.UserProfileService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('PASSENGER') or hasRole('DRIVER') or hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> getUserProfile(Authentication authentication) {
        String userEmail = authentication.getName();
        UserProfileResponse profile = userProfileService.getUserProfile(userEmail);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('PASSENGER') or hasRole('DRIVER')")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        UserProfileResponse updatedProfile = userProfileService.updateProfile(userEmail, request);
        return ResponseEntity.ok(updatedProfile);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('PASSENGER') or hasRole('DRIVER')")
    public ResponseEntity<UserDashboardResponse> getUserDashboard(Authentication authentication) {
        String userEmail = authentication.getName();
        UserDashboardResponse dashboard = userProfileService.getUserDashboard(userEmail);
        return ResponseEntity.ok(dashboard);
    }
}

package com.ashu.ride_sharing.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ashu.ride_sharing.dto.response.DriverStatusResponse;
import com.ashu.ride_sharing.models.DriverVerificationRequest;
import com.ashu.ride_sharing.services.DriverService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.security.core.Authentication; // MISSING 

@RestController
@RequestMapping("/api/v1/driver")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @PostMapping("/verify")
    @PreAuthorize("hasRole('DRIVER')")

    public ResponseEntity<String> verifyDriver(@Valid @RequestBody DriverVerificationRequest request,
            Authentication authentication) {
        String userEmail = authentication.getName();

        // if (!getVerificationStatus(authentication)){
        // return ResponseEntity.status(HttpStatus.OK)
        // .body("Driver verification completed! You can now add vehicles.");
        // };

        driverService.verifyDriver(userEmail, request);
        return ResponseEntity.status(HttpStatus.OK)
                .body("Driver verification completed! You can now add vehicles.");
    }

    @GetMapping("/verification-status")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<DriverStatusResponse> getVerificationStatus(Authentication authentication) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(driverService.getVerificationStatus(userEmail));

    }

}

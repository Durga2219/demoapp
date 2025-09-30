package com.ashu.ride_sharing.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.ashu.ride_sharing.dto.request.CreateRideRequest;
import com.ashu.ride_sharing.dto.response.RideResponse;
import com.ashu.ride_sharing.services.RideService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/rides")
@RequiredArgsConstructor
public class RideController {

    private final RideService rideService;

    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponse> createRide(
            @Valid @RequestBody CreateRideRequest request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        RideResponse response = rideService.createRide(userEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<RideResponse>> searchRides(
            @RequestParam String source,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<RideResponse> rides = rideService.searchRides(source, destination, date);
        return ResponseEntity.ok(rides);
    }

    @GetMapping("/my-rides")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<List<RideResponse>> getMyRides(Authentication authentication) {
        String userEmail = authentication.getName();
        List<RideResponse> rides = rideService.getDriverRides(userEmail);
        return ResponseEntity.ok(rides);
    }

    @GetMapping("/{rideId}")
    public ResponseEntity<RideResponse> getRideById(@PathVariable UUID rideId) {
        RideResponse ride = rideService.getRideById(rideId);
        return ResponseEntity.ok(ride);
    }

    @PutMapping("/{rideId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponse> updateRide(
            @PathVariable UUID rideId,
            @Valid @RequestBody CreateRideRequest request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        RideResponse response = rideService.updateRide(userEmail, rideId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{rideId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<String> cancelRide(
            @PathVariable UUID rideId,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        String userEmail = authentication.getName();
        rideService.cancelRide(userEmail, rideId, reason != null ? reason : "Cancelled by driver");
        return ResponseEntity.ok("Ride cancelled successfully");
    }
}
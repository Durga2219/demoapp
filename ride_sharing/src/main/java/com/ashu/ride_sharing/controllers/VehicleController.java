package com.ashu.ride_sharing.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ashu.ride_sharing.dto.request.VehicleRequest;
import com.ashu.ride_sharing.dto.response.VehicleResponse;
import com.ashu.ride_sharing.services.VehicleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<VehicleResponse> registerVehicle(
            @Valid @ModelAttribute VehicleRequest request,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        
        VehicleResponse response = vehicleService.registerVehicle(userEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-vehicles")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<List<VehicleResponse>> getMyVehicles(Authentication authentication) {
        String userEmail = authentication.getName();
        List<VehicleResponse> vehicles = vehicleService.getUserVehicles(userEmail);
        return ResponseEntity.ok(vehicles);
    }
}
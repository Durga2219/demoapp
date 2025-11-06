package com.example.demo.controller;

import com.example.demo.dto.FareRequest;
import com.example.demo.dto.FareResponse;
import com.example.demo.service.FareCalculationService;
import com.example.demo.service.RideService;
import com.example.demo.entity.Ride;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/fare")
@CrossOrigin(origins = "*")
public class FareController {

    private static final Logger logger = LoggerFactory.getLogger(FareController.class);

    @Autowired
    private FareCalculationService fareCalculationService;

    @Autowired
    private RideService rideService;

    /**
     * Calculate fare for a new ride
     * POST /api/fare/calculate
     */
    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculateFare(@Valid @RequestBody FareRequest fareRequest) {
        try {
            logger.info("Fare calculation request received: {}", fareRequest);

            FareResponse fareResponse = fareCalculationService.calculateFare(fareRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("success", fareResponse.isSuccess());
            response.put("message", fareResponse.getMessage());
            
            if (fareResponse.isSuccess()) {
                response.put("data", fareResponse.getFareDetails());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error in fare calculation: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to calculate fare: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Get fare estimate for an existing ride
     * GET /api/fare/estimate/{rideId}
     */
    @GetMapping("/estimate/{rideId}")
    public ResponseEntity<Map<String, Object>> getFareEstimate(@PathVariable Long rideId,
                                                              @RequestParam(defaultValue = "1") int passengers) {
        try {
            logger.info("Fare estimate request for ride {} with {} passengers", rideId, passengers);

            // Get ride details
            Ride ride = rideService.getRideById(rideId);
            if (ride == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Ride not found");
                return ResponseEntity.notFound().build();
            }

            // Calculate proportional fare for the ride
            FareResponse fareResponse = fareCalculationService.calculateProportionalFare(
                rideId, passengers, ride.getTotalSeats(), ride.getFare()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", fareResponse.isSuccess());
            response.put("message", fareResponse.getMessage());
            
            if (fareResponse.isSuccess()) {
                // Add ride details to the response
                Map<String, Object> estimateData = new HashMap<>();
                estimateData.put("rideId", rideId);
                estimateData.put("source", ride.getSource());
                estimateData.put("destination", ride.getDestination());
                estimateData.put("totalSeats", ride.getTotalSeats());
                estimateData.put("availableSeats", ride.getAvailableSeats());
                estimateData.put("pricePerKm", ride.getPricePerKm());
                estimateData.put("fareDetails", fareResponse.getFareDetails());
                
                response.put("data", estimateData);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting fare estimate: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get fare estimate: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Get quick fare estimate for route planning
     * GET /api/fare/quick-estimate
     */
    @GetMapping("/quick-estimate")
    public ResponseEntity<Map<String, Object>> getQuickEstimate(@RequestParam String source,
                                                               @RequestParam String destination,
                                                               @RequestParam(defaultValue = "1") int passengers) {
        try {
            logger.info("Quick fare estimate request: {} -> {} for {} passengers", source, destination, passengers);

            if (!fareCalculationService.validateFareParameters(source, destination, passengers)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Invalid parameters: source, destination, and passengers are required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            FareRequest fareRequest = new FareRequest(source, destination, passengers);
            FareResponse fareResponse = fareCalculationService.calculateFare(fareRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("success", fareResponse.isSuccess());
            response.put("message", fareResponse.getMessage());
            
            if (fareResponse.isSuccess()) {
                response.put("data", fareResponse.getFareDetails());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting quick fare estimate: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get quick estimate: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Get fare calculation constants for frontend display
     * GET /api/fare/constants
     */
    @GetMapping("/constants")
    public ResponseEntity<Map<String, Object>> getFareConstants() {
        try {
            Map<String, Object> constants = new HashMap<>();
            constants.put("baseFare", fareCalculationService.getDefaultBaseFare());
            constants.put("pricePerKm", fareCalculationService.getDefaultPricePerKm());
            constants.put("platformCommission", fareCalculationService.getPlatformCommissionRate());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Fare constants retrieved successfully");
            response.put("data", constants);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting fare constants: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get fare constants");
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Calculate driver earnings for a completed ride
     * GET /api/fare/driver-earnings/{rideId}
     */
    @GetMapping("/driver-earnings/{rideId}")
    public ResponseEntity<Map<String, Object>> getDriverEarnings(@PathVariable Long rideId) {
        try {
            logger.info("Driver earnings request for ride {}", rideId);

            // Get ride details
            Ride ride = rideService.getRideById(rideId);
            if (ride == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Ride not found");
                return ResponseEntity.notFound().build();
            }

            double totalFare = ride.getFare();
            double driverEarnings = fareCalculationService.calculateDriverEarnings(totalFare);
            double platformCommission = fareCalculationService.calculatePlatformCommission(totalFare);

            Map<String, Object> earningsData = new HashMap<>();
            earningsData.put("rideId", rideId);
            earningsData.put("totalFare", totalFare);
            earningsData.put("driverEarnings", driverEarnings);
            earningsData.put("platformCommission", platformCommission);
            earningsData.put("commissionRate", fareCalculationService.getPlatformCommissionRate());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Driver earnings calculated successfully");
            response.put("data", earningsData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error calculating driver earnings: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to calculate driver earnings: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Health check endpoint for fare service
     * GET /api/fare/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("service", "FareCalculationService");
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Fare service is healthy");
        response.put("data", health);

        return ResponseEntity.ok(response);
    }
}
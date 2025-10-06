package com.example.demo.controller;

import com.example.demo.dto.RideRequest;
import com.example.demo.dto.BookingRequest;
import com.example.demo.dto.ApiResponse;
import com.example.demo.entity.Ride;
import com.example.demo.entity.Booking;
import com.example.demo.entity.User;
import com.example.demo.service.RideService;
import com.example.demo.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/rides")
@CrossOrigin(origins = "*")
public class RideController {

    private static final Logger log = LoggerFactory.getLogger(RideController.class);

    @Autowired
    private RideService rideService;

    @Autowired
    private UserRepository userRepository; // Added to fetch driver

    // ---------------- DRIVER POSTS RIDE ----------------
    @PostMapping
    public ResponseEntity<ApiResponse> postRide(@Valid @RequestBody RideRequest request,
                                                BindingResult result,
                                                Authentication authentication) {
        if (result.hasErrors()) {
            String errorMsg = result.getFieldError().getDefaultMessage();
            log.warn("Post ride validation failed - {}", errorMsg);
            return ResponseEntity.badRequest().body(new ApiResponse(false, errorMsg, null));
        }

        try {
            String identifier = authentication.getName();

            // Try finding driver by email or username
            Optional<User> optionalDriver = userRepository.findByEmail(identifier);
            if (optionalDriver.isEmpty()) {
                optionalDriver = userRepository.findByUsername(identifier);
            }

            if (optionalDriver.isEmpty() || 
                (!optionalDriver.get().getRole().name().equals("DRIVER") && 
                 !optionalDriver.get().getRole().name().equals("BOTH"))) {
                log.warn("Driver not found or invalid role: {}", identifier);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(false, "Driver not found", null));
            }

            Ride ride = rideService.postRide(request, optionalDriver.get().getEmail());
            log.info("Ride posted successfully by {}", identifier);
            return ResponseEntity.ok(new ApiResponse(true, "Ride posted successfully", ride));
        } catch (Exception e) {
            log.error("Failed to post ride - error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ---------------- SEARCH RIDES ----------------
    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchRides(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer seats) {

        try {
            List<Ride> rides = rideService.searchRides(source, destination, date, seats);
            log.info("Rides retrieved for search params - source: {}, destination: {}, date: {}, seats: {}",
                     source, destination, date, seats);
            return ResponseEntity.ok(new ApiResponse(true, "Rides retrieved successfully", rides));
        } catch (Exception e) {
            log.error("Failed to search rides - error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ---------------- GET RIDE BY ID ----------------
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getRideById(@PathVariable Long id) {
        try {
            Ride ride = rideService.getRideById(id);
            log.info("Ride details retrieved - id: {}", id);
            return ResponseEntity.ok(new ApiResponse(true, "Ride details retrieved", ride));
        } catch (Exception e) {
            log.error("Ride not found - id: {}, error: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ---------------- BOOK A RIDE ----------------
    @PostMapping("/{id}/book")
    public ResponseEntity<ApiResponse> bookRide(@PathVariable Long id,
                                                @Valid @RequestBody BookingRequest request,
                                                BindingResult result,
                                                Authentication authentication) {
        if (result.hasErrors()) {
            String errorMsg = result.getFieldError().getDefaultMessage();
            log.warn("Book ride validation failed - {}", errorMsg);
            return ResponseEntity.badRequest().body(new ApiResponse(false, errorMsg, null));
        }

        try {
            String passengerEmail = authentication.getName();
            Booking booking = rideService.bookRide(id, request, passengerEmail);
            log.info("Ride booked successfully - rideId: {}, passenger: {}", id, passengerEmail);
            return ResponseEntity.ok(new ApiResponse(true, "Ride booked successfully", booking));
        } catch (Exception e) {
            log.error("Failed to book ride - rideId: {}, error: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ---------------- GET DRIVER RIDES ----------------
    @GetMapping("/my-rides")
    public ResponseEntity<ApiResponse> getMyRides(Authentication authentication) {
        try {
            String identifier = authentication.getName();

            Optional<User> optionalDriver = userRepository.findByEmail(identifier);
            if (optionalDriver.isEmpty()) optionalDriver = userRepository.findByUsername(identifier);

            if (optionalDriver.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(false, "Driver not found", null));
            }

            List<Ride> rides = rideService.getDriverRides(optionalDriver.get().getEmail());
            log.info("Driver rides retrieved - driver: {}", identifier);
            return ResponseEntity.ok(new ApiResponse(true, "Your rides retrieved", rides));
        } catch (Exception e) {
            log.error("Failed to get driver rides - error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ---------------- GET PASSENGER BOOKINGS ----------------
    @GetMapping("/my-bookings")
    public ResponseEntity<ApiResponse> getMyBookings(Authentication authentication) {
        try {
            String passengerEmail = authentication.getName();
            List<Booking> bookings = rideService.getPassengerBookings(passengerEmail);
            log.info("Passenger bookings retrieved - passenger: {}", passengerEmail);
            return ResponseEntity.ok(new ApiResponse(true, "Your bookings retrieved", bookings));
        } catch (Exception e) {
            log.error("Failed to get passenger bookings - error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ---------------- CANCEL BOOKING ----------------
    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<ApiResponse> cancelBooking(@PathVariable Long bookingId,
                                                     Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            rideService.cancelBooking(bookingId, userEmail);
            log.info("Booking cancelled successfully - bookingId: {}, user: {}", bookingId, userEmail);
            return ResponseEntity.ok(new ApiResponse(true, "Booking cancelled successfully", null));
        } catch (Exception e) {
            log.error("Failed to cancel booking - bookingId: {}, error: {}", bookingId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
}

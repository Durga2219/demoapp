package com.example.demo.controller;

import com.example.demo.dto.RideRequest;
import com.example.demo.dto.BookingRequest;
import com.example.demo.dto.ApiResponse;
import com.example.demo.entity.Ride;
import com.example.demo.entity.Booking;
import com.example.demo.service.RideService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rides")
@CrossOrigin(origins = "*")
public class RideController {

    @Autowired
    private RideService rideService;

    // Driver posts a ride
    @PostMapping
    public ResponseEntity<ApiResponse> postRide(
            @RequestBody RideRequest request,
            Authentication authentication) {
        try {
            String driverEmail = authentication.getName();
            Ride ride = rideService.postRide(request, driverEmail);
            return ResponseEntity.ok(new ApiResponse(true, "Ride posted successfully", ride));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // Search rides
    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchRides(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<Ride> rides = rideService.searchRides(source, destination, date);
            return ResponseEntity.ok(new ApiResponse(true, "Rides retrieved successfully", rides));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // Get ride by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getRideById(@PathVariable Long id) {
        try {
            Ride ride = rideService.getRideById(id);
            return ResponseEntity.ok(new ApiResponse(true, "Ride details retrieved", ride));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // Book a ride
    @PostMapping("/{id}/book")
    public ResponseEntity<ApiResponse> bookRide(
            @PathVariable Long id,
            @RequestBody BookingRequest request,
            Authentication authentication) {
        try {
            String passengerEmail = authentication.getName();
            Booking booking = rideService.bookRide(id, request, passengerEmail);
            return ResponseEntity.ok(new ApiResponse(true, "Ride booked successfully", booking));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // Get driver's posted rides
    @GetMapping("/my-rides")
    public ResponseEntity<ApiResponse> getMyRides(Authentication authentication) {
        try {
            String driverEmail = authentication.getName();
            List<Ride> rides = rideService.getDriverRides(driverEmail);
            return ResponseEntity.ok(new ApiResponse(true, "Your rides retrieved", rides));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // Get passenger's bookings
    @GetMapping("/my-bookings")
    public ResponseEntity<ApiResponse> getMyBookings(Authentication authentication) {
        try {
            String passengerEmail = authentication.getName();
            List<Booking> bookings = rideService.getPassengerBookings(passengerEmail);
            return ResponseEntity.ok(new ApiResponse(true, "Your bookings retrieved", bookings));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // Cancel booking
    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<ApiResponse> cancelBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            rideService.cancelBooking(bookingId, userEmail);
            return ResponseEntity.ok(new ApiResponse(true, "Booking cancelled successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
}
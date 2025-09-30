package com.ashu.ride_sharing.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.ashu.ride_sharing.dto.request.BookingRequest;
import com.ashu.ride_sharing.dto.response.BookingResponse;
import com.ashu.ride_sharing.services.BookingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasRole('PASSENGER') or hasRole('DRIVER')")
    public ResponseEntity<BookingResponse> bookRide(
            @Valid @RequestBody BookingRequest request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        BookingResponse response = bookingService.bookRide(userEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-bookings")
    @PreAuthorize("hasRole('PASSENGER') or hasRole('DRIVER')")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Authentication authentication) {
        String userEmail = authentication.getName();
        List<BookingResponse> bookings = bookingService.getPassengerBookings(userEmail);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/driver-bookings")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<List<BookingResponse>> getDriverBookings(Authentication authentication) {
        String userEmail = authentication.getName();
        List<BookingResponse> bookings = bookingService.getDriverBookings(userEmail);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/{bookingId}")
    @PreAuthorize("hasRole('PASSENGER') or hasRole('DRIVER')")
    public ResponseEntity<BookingResponse> getBookingById(
            @PathVariable UUID bookingId,
            Authentication authentication) {
        String userEmail = authentication.getName();
        BookingResponse booking = bookingService.getBookingById(userEmail, bookingId);
        return ResponseEntity.ok(booking);
    }

    @PutMapping("/{bookingId}/confirm")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<BookingResponse> confirmBooking(
            @PathVariable UUID bookingId,
            Authentication authentication) {
        String userEmail = authentication.getName();
        BookingResponse response = bookingService.confirmBooking(userEmail, bookingId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{bookingId}/cancel")
    @PreAuthorize("hasRole('PASSENGER') or hasRole('DRIVER')")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable UUID bookingId,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        String userEmail = authentication.getName();
        BookingResponse response = bookingService.cancelBooking(userEmail, bookingId, 
                reason != null ? reason : "Cancelled by user");
        return ResponseEntity.ok(response);
    }
}
package com.example.demo.controller;

import com.example.demo.dto.RideRequest;
import com.example.demo.dto.BookingRequest;
import com.example.demo.dto.ApiResponse;
import com.example.demo.entity.Ride;
import com.example.demo.entity.Booking;
import com.example.demo.entity.User;
import com.example.demo.service.RideService;
import com.example.demo.service.RideStatusService;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.RideRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/rides")
@CrossOrigin(origins = "*")
public class RideController {

    private static final Logger log = LoggerFactory.getLogger(RideController.class);

    @Autowired
    private RideService rideService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RideRepository rideRepository;

    // Simple in-memory notification system for real-time updates
    private static final Map<String, String> userNotifications = new ConcurrentHashMap<>();

    // ==================== DRIVER POSTS RIDE ====================
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
            Optional<User> optionalDriver = userRepository.findByEmail(identifier);
            if (optionalDriver.isEmpty()) optionalDriver = userRepository.findByUsername(identifier);

            if (optionalDriver.isEmpty() || (!optionalDriver.get().getRole().name().equals("DRIVER") &&
                                              !optionalDriver.get().getRole().name().equals("BOTH"))) {
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

    // ==================== SEARCH RIDES (GET) ====================
    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchRides(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer seats) {

        try {
            List<Ride> rides = rideService.searchRides(source, destination, date, seats);
            return ResponseEntity.ok(new ApiResponse(true, "Rides retrieved successfully", rides));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ==================== SEARCH RIDES (POST) ====================
    @PostMapping("/search")
    public ResponseEntity<ApiResponse> searchRidesPost(@RequestBody Map<String, Object> searchRequest) {
        try {
            String source = (String) searchRequest.get("source");
            String destination = (String) searchRequest.get("destination");
            String dateStr = (String) searchRequest.get("date");
            Integer seats = searchRequest.get("passengers") != null ? 
                Integer.parseInt(searchRequest.get("passengers").toString()) : null;
            
            LocalDate date = null;
            if (dateStr != null && !dateStr.isEmpty()) {
                date = LocalDate.parse(dateStr);
            }
            
            List<Ride> rides = rideService.searchRides(source, destination, date, seats);
            return ResponseEntity.ok(new ApiResponse(true, "Rides retrieved successfully", rides));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ==================== ENHANCED SEARCH WITH ROUTE MATCHING ====================
    @GetMapping("/search/enhanced")
    public ResponseEntity<ApiResponse> searchRidesEnhanced(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer seats) {

        try {
            List<Map<String, Object>> ridesWithMatches = rideService.searchRidesWithMatchDetails(source, destination, date, seats);
            
            Map<String, Object> response = new HashMap<>();
            response.put("rides", ridesWithMatches);
            response.put("totalMatches", ridesWithMatches.size());
            response.put("searchCriteria", Map.of(
                "source", source != null ? source : "Any",
                "destination", destination != null ? destination : "Any", 
                "date", date != null ? date.toString() : "Any",
                "seats", seats != null ? seats : "Any"
            ));
            
            log.info("🔍 Enhanced search completed - Found {} matches for {} -> {}", 
                     ridesWithMatches.size(), source, destination);
            
            return ResponseEntity.ok(new ApiResponse(true, "Enhanced search completed successfully", response));
        } catch (Exception e) {
            log.error("❌ Error in enhanced search: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    @PostMapping("/search/enhanced")
    public ResponseEntity<ApiResponse> searchRidesEnhancedPost(@RequestBody Map<String, Object> searchRequest) {
        try {
            String source = (String) searchRequest.get("source");
            String destination = (String) searchRequest.get("destination");
            String dateStr = (String) searchRequest.get("date");
            Integer seats = searchRequest.get("passengers") != null ? 
                Integer.parseInt(searchRequest.get("passengers").toString()) : null;
            
            LocalDate date = null;
            if (dateStr != null && !dateStr.isEmpty()) {
                date = LocalDate.parse(dateStr);
            }
            
            List<Map<String, Object>> ridesWithMatches = rideService.searchRidesWithMatchDetails(source, destination, date, seats);
            
            Map<String, Object> response = new HashMap<>();
            response.put("rides", ridesWithMatches);
            response.put("totalMatches", ridesWithMatches.size());
            response.put("searchCriteria", Map.of(
                "source", source != null ? source : "Any",
                "destination", destination != null ? destination : "Any", 
                "date", date != null ? date.toString() : "Any",
                "seats", seats != null ? seats : "Any"
            ));
            
            return ResponseEntity.ok(new ApiResponse(true, "Enhanced search completed successfully", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ==================== BOOK A RIDE ====================
    @PostMapping("/{id}/book")
    public ResponseEntity<ApiResponse> bookRide(@PathVariable Long id,
                                                @Valid @RequestBody BookingRequest request,
                                                BindingResult result,
                                                Authentication authentication) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, result.getFieldError().getDefaultMessage(), null));
        }

        try {
            String passengerEmail = authentication.getName();
            Booking booking = rideService.bookRide(id, request, passengerEmail);
            log.info("✅ Booking successful - Passenger: {}, Ride: {}, Fare: ₹{}", 
                     passengerEmail, id, booking.getFare());
            
            // Real-time notification to driver
            String driverEmail = booking.getRide().getDriver().getEmail();
            String notification = String.format("New booking! %s booked %d seat(s) for your ride from %s to %s", 
                                               passengerEmail, booking.getSeatsBooked(), 
                                               booking.getRide().getSource(), booking.getRide().getDestination());
            userNotifications.put(driverEmail, notification);
            
            return ResponseEntity.ok(new ApiResponse(true, "Ride booked successfully", booking));
        } catch (Exception e) {
            log.error("❌ Booking failed - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ==================== BOOK A RIDE (Alternative endpoint) ====================
    @PostMapping("/book")
    public ResponseEntity<ApiResponse> bookRideAlternative(@RequestBody Map<String, Object> bookingRequest,
                                                          Authentication authentication) {
        try {
            String passengerEmail = authentication.getName();
            Long rideId = Long.valueOf(bookingRequest.get("rideId").toString());
            Integer seatsToBook = Integer.valueOf(bookingRequest.get("seatsToBook").toString());
            
            // Create BookingRequest object
            BookingRequest request = new BookingRequest();
            request.setSeatsBooked(seatsToBook);
            
            Booking booking = rideService.bookRide(rideId, request, passengerEmail);
            log.info("✅ Booking successful - Passenger: {}, Ride: {}, Fare: ₹{}", 
                     passengerEmail, rideId, booking.getFare());
            
            // Real-time notification to driver
            String driverEmail = booking.getRide().getDriver().getEmail();
            String notification = String.format("New booking! %s booked %d seat(s) for your ride from %s to %s", 
                                               passengerEmail, booking.getSeatsBooked(), 
                                               booking.getRide().getSource(), booking.getRide().getDestination());
            userNotifications.put(driverEmail, notification);
            
            return ResponseEntity.ok(new ApiResponse(true, "Ride booked successfully", booking));
        } catch (Exception e) {
            log.error("❌ Booking failed - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ==================== NEW: CALCULATE FARE ====================
    @GetMapping("/{id}/calculate-fare")
    public ResponseEntity<ApiResponse> calculateFare(
            @PathVariable Long id,
            @RequestParam(required = false) Double distance,
            @RequestParam(defaultValue = "1") Integer seats) {
        try {
            Map<String, Object> fareDetails = rideService.calculateFare(id, distance, seats);
            return ResponseEntity.ok(new ApiResponse(true, "Fare calculated", fareDetails));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ==================== GET RIDE DETAILS (PUBLIC - NO AUTH REQUIRED) ====================
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getRideDetails(@PathVariable Long id) {
        try {
            log.info("🔍 Fetching ride details for ID: {}", id);
            
            Optional<Ride> rideOpt = rideRepository.findById(id);
            if (rideOpt.isEmpty()) {
                log.warn("❌ Ride not found with ID: {}", id);
                return ResponseEntity.status(404)
                        .body(new ApiResponse(false, "Ride not found", null));
            }

            Ride ride = rideOpt.get();
            log.info("✅ Found ride: {} -> {} on {}", ride.getSource(), ride.getDestination(), ride.getDate());
            
            // Create response with ride details
            Map<String, Object> rideDetails = new HashMap<>();
            rideDetails.put("id", ride.getId());
            rideDetails.put("source", ride.getSource());
            rideDetails.put("destination", ride.getDestination());
            rideDetails.put("date", ride.getDate());
            rideDetails.put("time", ride.getTime());
            rideDetails.put("totalSeats", ride.getTotalSeats());
            rideDetails.put("availableSeats", ride.getAvailableSeats());
            rideDetails.put("pricePerKm", ride.getPricePerKm());
            rideDetails.put("fare", ride.getFare());
            rideDetails.put("distanceKm", ride.getDistanceKm()); // Distance calculated during ride creation
            rideDetails.put("vehicleModel", ride.getVehicleModel());
            rideDetails.put("vehiclePlate", ride.getVehiclePlate());
            rideDetails.put("status", ride.getStatus());
            rideDetails.put("contactNumber", ride.getContactNumber());
            
            // Add driver details - CRITICAL for booking modal
            if (ride.getDriver() != null) {
                Map<String, Object> driverInfo = new HashMap<>();
                driverInfo.put("id", ride.getDriver().getId());
                driverInfo.put("username", ride.getDriver().getUsername());
                driverInfo.put("email", ride.getDriver().getEmail());
                driverInfo.put("phone", ride.getDriver().getPhone());
                driverInfo.put("profilePicture", ride.getDriver().getProfilePicture());
                driverInfo.put("rating", ride.getDriver().getRating() != null ? ride.getDriver().getRating() : 4.5);
                rideDetails.put("driver", driverInfo);
                log.info("✅ Driver info added: {}", ride.getDriver().getUsername());
            } else {
                log.error("❌❌❌ CRITICAL: Ride {} has NO DRIVER assigned!", id);
            }

            log.info("✅ Returning ride details with {} fields", rideDetails.size());
            return ResponseEntity.ok(new ApiResponse(true, "Ride details retrieved successfully", rideDetails));

        } catch (Exception e) {
            log.error("❌ Error getting ride details: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse(false, "Error retrieving ride details: " + e.getMessage(), null));
        }
    }

    // ==================== GET DRIVER RIDES ====================
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
            return ResponseEntity.ok(new ApiResponse(true, "Your rides retrieved", rides));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ==================== GET PASSENGER BOOKINGS ====================
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

    // ==================== CANCEL BOOKING ====================
    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<ApiResponse> cancelBooking(@PathVariable Long bookingId,
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

    // ==================== CANCEL RIDE (DRIVER) ====================
    @PutMapping("/{rideId}/cancel")
    public ResponseEntity<ApiResponse> cancelRide(@PathVariable Long rideId,
                                                   Authentication authentication) {
        try {
            String driverEmail = authentication.getName();
            rideService.cancelRide(rideId, driverEmail);
            return ResponseEntity.ok(new ApiResponse(true, "Ride cancelled successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ==================== UPDATE RIDE ====================
    @PutMapping("/{rideId}")
    public ResponseEntity<ApiResponse> updateRide(@PathVariable Long rideId,
                                                  @RequestBody Map<String, Object> updateRequest,
                                                  Authentication authentication) {
        try {
            String driverEmail = authentication.getName();
            
            // Get the ride and verify ownership
            Optional<Ride> rideOpt = rideRepository.findById(rideId);
            if (rideOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(new ApiResponse(false, "Ride not found", null));
            }
            
            Ride ride = rideOpt.get();
            
            // Verify the driver owns this ride
            if (!ride.getDriver().getEmail().equals(driverEmail) && 
                !ride.getDriver().getUsername().equals(driverEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "You can only update your own rides", null));
            }
            
            // Update fields if provided
            if (updateRequest.containsKey("availableSeats")) {
                Integer availableSeats = Integer.valueOf(updateRequest.get("availableSeats").toString());
                if (availableSeats > 0 && availableSeats <= (ride.getTotalSeats() != null ? ride.getTotalSeats() : 8)) {
                    ride.setAvailableSeats(availableSeats);
                }
            }
            
            if (updateRequest.containsKey("pricePerKm")) {
                Double pricePerKm = Double.valueOf(updateRequest.get("pricePerKm").toString());
                if (pricePerKm > 0) {
                    ride.setPricePerKm(pricePerKm);
                }
            }
            
            if (updateRequest.containsKey("status")) {
                String status = updateRequest.get("status").toString();
                if ("ACTIVE".equals(status) || "CANCELLED".equals(status)) {
                    ride.setStatus(status);
                }
            }
            
            // Save updated ride
            Ride updatedRide = rideRepository.save(ride);
            
            log.info("Ride {} updated successfully by {}", rideId, driverEmail);
            return ResponseEntity.ok(new ApiResponse(true, "Ride updated successfully", updatedRide));
            
        } catch (Exception e) {
            log.error("Error updating ride {}: {}", rideId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ==================== DELETE RIDE ====================
    @DeleteMapping("/{rideId}")
    public ResponseEntity<ApiResponse> deleteRide(@PathVariable Long rideId,
                                                   Authentication authentication) {
        try {
            String driverEmail = authentication.getName();
            rideService.deleteRide(rideId, driverEmail);
            return ResponseEntity.ok(new ApiResponse(true, "Ride deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ==================== GET RIDE BOOKINGS (DRIVER) ====================
    @GetMapping("/{rideId}/bookings")
    public ResponseEntity<ApiResponse> getRideBookings(@PathVariable Long rideId,
                                                        Authentication authentication) {
        try {
            List<Booking> bookings = rideService.getRideBookings(rideId);
            return ResponseEntity.ok(new ApiResponse(true, "Bookings retrieved", bookings));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ==================== GET DRIVER BOOKINGS ====================
    @GetMapping("/bookings/driver")
    public ResponseEntity<ApiResponse> getDriverBookings(Authentication authentication) {
        try {
            String driverEmail = authentication.getName();
            Optional<User> driverOpt = userRepository.findByEmail(driverEmail);
            if (driverOpt.isEmpty()) driverOpt = userRepository.findByUsername(driverEmail);

            if (driverOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Driver not found", null));
            }

            User driver = driverOpt.get();
            
            // Get all bookings for this driver's rides
            List<Booking> driverBookings = rideService.getDriverBookings(driver.getId());
            
            log.info("Found {} bookings for driver: {}", driverBookings.size(), driverEmail);
            return ResponseEntity.ok(new ApiResponse(true, "Driver bookings retrieved successfully", driverBookings));

        } catch (Exception e) {
            log.error("Error getting driver bookings: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse(false, "Error retrieving driver bookings", null));
        }
    }

    // ==================== REAL-TIME NOTIFICATIONS ====================
    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse> getNotifications(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            String notification = userNotifications.get(userEmail);
            
            Map<String, Object> response = new HashMap<>();
            response.put("hasNotification", notification != null);
            response.put("message", notification);
            
            // Clear notification after reading
            if (notification != null) {
                userNotifications.remove(userEmail);
            }
            
            return ResponseEntity.ok(new ApiResponse(true, "Notifications retrieved", response));
            
        } catch (Exception e) {
            log.error("Error getting notifications: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(new ApiResponse(false, "Error retrieving notifications", null));
        }
    }

    // ==================== AVAILABLE RIDES FOR PASSENGERS ====================
    @GetMapping("/available")
    public ResponseEntity<ApiResponse> getAvailableRides() {
        try {
            List<Ride> availableRides = rideService.getAllAvailableRides();
            return ResponseEntity.ok(new ApiResponse(true, "Available rides retrieved", availableRides));

        } catch (Exception e) {
            log.error("Error getting available rides: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to get available rides", null));
        }
    }

    // ==================== UPDATE RIDE STATUSES ====================
    @PostMapping("/update-statuses")
    public ResponseEntity<ApiResponse> updateRideStatuses() {
        try {
            int updatedCount = rideService.updateRideStatuses();
            return ResponseEntity.ok(new ApiResponse(true, 
                "Updated " + updatedCount + " rides to completed status", 
                Map.of("updatedCount", updatedCount)));

        } catch (Exception e) {
            log.error("Error updating ride statuses: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to update ride statuses", null));
        }
    }

    // ==================== MARK BOOKING AS COMPLETED ====================
    @PutMapping("/bookings/{bookingId}/complete")
    public ResponseEntity<ApiResponse> markBookingAsCompleted(
            @PathVariable Long bookingId,
            Authentication authentication) {
        try {
            String userIdentifier = authentication.getName();
            Booking completedBooking = rideService.markBookingAsCompleted(bookingId, userIdentifier);
            
            log.info("✅ Booking {} marked as completed by {}", bookingId, userIdentifier);
            return ResponseEntity.ok(new ApiResponse(true, 
                "Booking marked as completed. Please rate your experience!", 
                completedBooking));

        } catch (Exception e) {
            log.error("❌ Error marking booking as completed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ==================== MARK RIDE AS COMPLETED ====================
    @PutMapping("/{rideId}/complete")
    public ResponseEntity<ApiResponse> markRideAsCompleted(
            @PathVariable Long rideId,
            Authentication authentication) {
        try {
            String driverIdentifier = authentication.getName();
            Ride completedRide = rideService.markRideAsCompleted(rideId, driverIdentifier);
            
            log.info("✅ Ride {} marked as completed by driver {}", rideId, driverIdentifier);
            return ResponseEntity.ok(new ApiResponse(true, 
                "Ride marked as completed. All bookings have been updated.", 
                completedRide));

        } catch (Exception e) {
            log.error("❌ Error marking ride as completed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ==================== RIDE STATISTICS ====================
    @GetMapping("/stats/global")
    public ResponseEntity<ApiResponse> getGlobalRideStats() {
        try {
            RideStatusService.RideStats stats = rideService.getGlobalRideStats();
            
            // Convert to Map for response
            Map<String, Object> statsMap = new HashMap<>();
            statsMap.put("totalRides", stats.getTotalRides());
            statsMap.put("activeRides", stats.getActiveRides());
            statsMap.put("completedRides", stats.getCompletedRides());
            statsMap.put("cancelledRides", stats.getCancelledRides());
            
            return ResponseEntity.ok(new ApiResponse(true, "Global ride stats retrieved", statsMap));

        } catch (Exception e) {
            log.error("Error getting global ride stats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to get ride stats", null));
        }
    }

    // ==================== SIMPLE BOOKING ENDPOINT FOR FRONTEND ====================
    @PostMapping("/bookings")
    public ResponseEntity<ApiResponse> createBooking(@RequestBody Map<String, Object> bookingData) {
        try {
            log.info("🎫 Received booking request: {}", bookingData);
            
            // Extract booking data
            Long rideId = Long.valueOf(bookingData.get("rideId").toString());
            Integer seatsBooked = Integer.valueOf(bookingData.get("seatsBooked").toString());
            Double totalAmount = Double.valueOf(bookingData.get("totalAmount").toString());
            
            // For demo purposes, create a simple booking response
            Map<String, Object> bookingResponse = new HashMap<>();
            bookingResponse.put("bookingId", System.currentTimeMillis()); // Simple ID generation
            bookingResponse.put("rideId", rideId);
            bookingResponse.put("seatsBooked", seatsBooked);
            bookingResponse.put("totalAmount", totalAmount);
            bookingResponse.put("status", "CONFIRMED");
            bookingResponse.put("bookingTime", java.time.LocalDateTime.now());
            bookingResponse.put("passengerEmail", "demo@passenger.com"); // Demo email
            
            log.info("✅ Booking created successfully: ID {}", bookingResponse.get("bookingId"));
            
            return ResponseEntity.ok(new ApiResponse(true, "Booking confirmed successfully!", bookingResponse));
            
        } catch (Exception e) {
            log.error("❌ Booking failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Booking failed: " + e.getMessage(), null));
        }
    }
}
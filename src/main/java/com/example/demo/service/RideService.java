package com.example.demo.service;

import com.example.demo.controller.AdminWebSocketController;
import com.example.demo.controller.WebSocketNotificationController;
import com.example.demo.dto.RideRequest;
import com.example.demo.dto.BookingRequest;
import com.example.demo.entity.Ride;
import com.example.demo.entity.Booking;
import com.example.demo.entity.User;
import com.example.demo.enums.BookingStatus;
import com.example.demo.repository.RideRepository;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RideService {

    private static final Logger logger = LoggerFactory.getLogger(RideService.class);

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RouteMatchingService routeMatchingService;

    @Autowired
    private AdminWebSocketController adminWebSocketController;

    @Autowired
    private WebSocketNotificationController webSocketNotificationController;

    @Autowired
    private NotificationService notificationService;

    private static final double BASE_FARE = 50.0;
    private static final String OPENROUTESERVICE_KEY = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjQ5OTQ1MTk5ZDBlNDRiNjFhOTRmZDNjMjFjMjM5ZGUwIiwiaCI6Im11cm11cjY0In0=";
    private static final String OPENROUTESERVICE_URL = "https://api.openrouteservice.org/v2/directions/driving-car";

    private RestTemplate restTemplate = new RestTemplate();

    // ---------------- POST RIDE ----------------
    @Transactional
    public Ride postRide(RideRequest request, String driverIdentifier) {
        logger.info("🔍 Creating ride for driver: {}", driverIdentifier);
        
        User driver = userRepository.findByEmail(driverIdentifier)
                .orElseGet(() -> userRepository.findByUsername(driverIdentifier)
                        .orElseThrow(() -> new RuntimeException("Driver not found")));

        logger.info("👤 Driver found: ID={}, Email={}, Username={}, Role={}", 
                   driver.getId(), driver.getEmail(), driver.getUsername(), driver.getRole());

        if (!(driver.getRole().name().equals("DRIVER") || driver.getRole().name().equals("BOTH"))) {
            throw new RuntimeException("Only drivers can post rides");
        }

        Ride ride = new Ride();
        ride.setDriver(driver);
        ride.setSource(request.getSource());
        ride.setDestination(request.getDestination());
        ride.setDate(request.getDate());
        ride.setTime(request.getTime());
        ride.setAvailableSeats(request.getAvailableSeats());
        ride.setTotalSeats(request.getTotalSeats());
        ride.setVehicleModel(request.getVehicleModel() != null ? request.getVehicleModel() : driver.getVehicleModel());
        ride.setVehiclePlate(request.getVehiclePlate() != null ? request.getVehiclePlate() : driver.getVehiclePlate());
        ride.setContactNumber(request.getContactNumber() != null ? request.getContactNumber() : driver.getPhone());
        ride.setNotes(request.getNotes());
        ride.setPricePerKm(request.getPricePerKm() != null ? request.getPricePerKm() : 10.0);
        ride.setStatus("ACTIVE");

        // -------- Dynamic Fare Calculation --------
        double distanceKm = getDistanceKm(request.getSource(), request.getDestination());
        ride.setDistanceKm(Math.round(distanceKm * 100.0) / 100.0); // Store distance in ride
        double totalRideFare = BASE_FARE + ride.getPricePerKm() * distanceKm;
        // Store FARE PER SEAT (not total ride fare)
        double farePerSeat = totalRideFare / ride.getTotalSeats();
        ride.setFare(Math.round(farePerSeat * 100.0) / 100.0);
        
        logger.info("📊 Calculated distance: {} km, Total ride fare: ₹{}, Per seat: ₹{}", 
            distanceKm, Math.round(totalRideFare * 100.0) / 100.0, ride.getFare());

        Ride savedRide = rideRepository.save(ride);
        
        logger.info("✅ Ride saved successfully: ID={}, Driver ID={}, {} → {}", 
                   savedRide.getId(), savedRide.getDriver().getId(), 
                   savedRide.getSource(), savedRide.getDestination());
        
        // Notify admin of new ride creation
        try {
            adminWebSocketController.notifyNewRide(savedRide);
        } catch (Exception e) {
            logger.warn("Failed to send admin notification for new ride: " + e.getMessage());
        }
        
        return savedRide;
    }

    // ---------------- BOOK RIDE ----------------
    @Transactional
    public Booking bookRide(Long rideId, BookingRequest request, String passengerIdentifier) {
        User passenger = userRepository.findByEmail(passengerIdentifier)
                .orElseGet(() -> userRepository.findByUsername(passengerIdentifier)
                        .orElseThrow(() -> new RuntimeException("Passenger not found")));

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        if (ride.getAvailableSeats() < request.getSeatsBooked()) {
            throw new RuntimeException("Not enough seats available");
        }

        if (ride.getDriver().getId().equals(passenger.getId())) {
            throw new RuntimeException("Driver cannot book own ride");
        }

        Booking booking = new Booking();
        booking.setRide(ride);
        booking.setPassenger(passenger);
        booking.setSeatsBooked(request.getSeatsBooked());
        booking.setPickupLocation(request.getPickupLocation() != null ? request.getPickupLocation() : ride.getSource());
        booking.setDropLocation(request.getDropLocation() != null ? request.getDropLocation() : ride.getDestination());
        booking.setStatus(BookingStatus.PENDING); // Changed to pending payment

        // -------- DYNAMIC FARE CALCULATION BASED ON DISTANCE --------
        // Use the ride's pre-calculated fare per seat
        // Fare per seat is already stored in ride.getFare()
        double farePerSeat = ride.getFare();
        double bookingDistance = ride.getDistanceKm(); // Default to ride distance
        
        // If pickup/drop differ from ride source/destination, recalculate
        String pickupLoc = booking.getPickupLocation();
        String dropLoc = booking.getDropLocation();
        
        if (!pickupLoc.equalsIgnoreCase(ride.getSource()) || !dropLoc.equalsIgnoreCase(ride.getDestination())) {
            // Recalculate for custom route
            double actualDistanceKm = getDistanceKm(pickupLoc, dropLoc);
            bookingDistance = actualDistanceKm;
            double actualTotalFare = BASE_FARE + (ride.getPricePerKm() * actualDistanceKm);
            farePerSeat = actualTotalFare / ride.getTotalSeats();
            logger.info("📊 Custom route: {} -> {} = {} km, Fare per seat: ₹{}", 
                pickupLoc, dropLoc, actualDistanceKm, Math.round(farePerSeat * 100.0) / 100.0);
        }
        
        // Store distance in booking
        booking.setDistance(Math.round(bookingDistance * 100.0) / 100.0);
        
        // Calculate total fare for the number of seats booked
        double passengerFare = farePerSeat * request.getSeatsBooked();
        
        // Round to 2 decimal places
        passengerFare = Math.round(passengerFare * 100.0) / 100.0;
        booking.setFare(passengerFare);
        
        logger.info("📊 Booking fare: ₹{} per seat × {} seat(s) = ₹{}", 
            Math.round(farePerSeat * 100.0) / 100.0, request.getSeatsBooked(), passengerFare);

        // Don't reduce available seats yet - wait for payment confirmation
        return bookingRepository.save(booking);
    }

    // ---------------- DRIVER RIDES ----------------
    public List<Ride> getDriverRides(String driverIdentifier) {
        logger.info("🔍 Getting rides for driver: {}", driverIdentifier);
        
        User driver = userRepository.findByEmail(driverIdentifier)
                .orElseGet(() -> userRepository.findByUsername(driverIdentifier)
                        .orElseThrow(() -> new RuntimeException("Driver not found")));
        
        logger.info("👤 Found driver: ID={}, Email={}, Username={}", driver.getId(), driver.getEmail(), driver.getUsername());
        
        List<Ride> rides = rideRepository.findByDriverId(driver.getId());
        logger.info("🚗 Found {} rides for driver ID {}", rides.size(), driver.getId());
        
        if (rides.isEmpty()) {
            logger.warn("⚠️ No rides found for driver {}. This could mean:", driverIdentifier);
            logger.warn("   - The driver hasn't created any rides yet");
            logger.warn("   - The driver ID might not match the rides in the database");
            logger.warn("   - Check the rides table for driver_id = {}", driver.getId());
        }
        
        return rides;
    }

    // ---------------- CONFIRM BOOKING AFTER PAYMENT ----------------
    @Transactional
    public Booking confirmBookingAfterPayment(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Booking is not in pending payment status");
        }

        Ride ride = booking.getRide();
        
        // Check if seats are still available
        if (ride.getAvailableSeats() < booking.getSeatsBooked()) {
            throw new RuntimeException("Seats no longer available");
        }

        // Confirm booking and reduce available seats
        booking.setStatus(BookingStatus.CONFIRMED);
        ride.setAvailableSeats(ride.getAvailableSeats() - booking.getSeatsBooked());
        
        if (ride.getAvailableSeats() == 0) {
            ride.setStatus("FULL");
            // Broadcast ride status update when ride becomes full
            try {
                Map<String, Object> statusUpdate = new HashMap<>();
                statusUpdate.put("rideId", ride.getId());
                statusUpdate.put("oldStatus", "ACTIVE");
                statusUpdate.put("newStatus", "FULL");
                statusUpdate.put("timestamp", LocalDateTime.now());
                statusUpdate.put("source", ride.getSource());
                statusUpdate.put("destination", ride.getDestination());
                statusUpdate.put("driverName", ride.getDriver().getName());

                webSocketNotificationController.sendRideStatusUpdate(ride.getDriver().getUsername(), statusUpdate);

                // Send to confirmed passengers
                List<Booking> confirmedBookings = bookingRepository.findByRideId(ride.getId());
                for (Booking b : confirmedBookings) {
                    if (b.getStatus() == BookingStatus.CONFIRMED) {
                        webSocketNotificationController.sendRideStatusUpdate(b.getPassenger().getUsername(), statusUpdate);
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to broadcast ride status update: " + e.getMessage());
            }
        }

        rideRepository.save(ride);
        return bookingRepository.save(booking);
    }

    // ---------------- PASSENGER BOOKINGS ----------------
    public List<Booking> getPassengerBookings(String passengerIdentifier) {
        User passenger = userRepository.findByEmail(passengerIdentifier)
                .orElseGet(() -> userRepository.findByUsername(passengerIdentifier)
                        .orElseThrow(() -> new RuntimeException("Passenger not found")));
        return bookingRepository.findByPassengerId(passenger.getId());
    }

    // ---------------- GET BOOKING BY ID ----------------
    public Booking getBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
    }

    // ---------------- CANCEL BOOKING ----------------
    @Transactional
    public void cancelBooking(Long bookingId, String userIdentifier) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        User user = userRepository.findByEmail(userIdentifier)
                .orElseGet(() -> userRepository.findByUsername(userIdentifier)
                        .orElseThrow(() -> new RuntimeException("User not found")));

        if (!booking.getPassenger().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to cancel this booking");
        }

        // Only restore seats if booking was confirmed (seats were actually reserved)
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            Ride ride = booking.getRide();
            ride.setAvailableSeats(ride.getAvailableSeats() + booking.getSeatsBooked());
            ride.setStatus("ACTIVE");
            rideRepository.save(ride);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);
    }

    // ---------------- CANCEL RIDE (DRIVER) ----------------
    @Transactional
    public void cancelRide(Long rideId, String driverIdentifier) {
        User driver = userRepository.findByEmail(driverIdentifier)
                .orElseGet(() -> userRepository.findByUsername(driverIdentifier)
                        .orElseThrow(() -> new RuntimeException("Driver not found")));
        
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        
        if (!ride.getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("Unauthorized to cancel this ride");
        }
        
        if (ride.getStatus().equals("CANCELLED")) {
            throw new RuntimeException("Ride is already cancelled");
        }
        
        // Cancel all bookings for this ride
        List<Booking> bookings = bookingRepository.findByRideId(rideId);
        for (Booking booking : bookings) {
            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                booking.setStatus(BookingStatus.CANCELLED);
                booking.setCancelledAt(LocalDateTime.now());
                bookingRepository.save(booking);
            }
        }
        
        ride.setStatus("CANCELLED");
        rideRepository.save(ride);

        // Broadcast ride status update when ride is cancelled
        try {
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("rideId", ride.getId());
            statusUpdate.put("oldStatus", "ACTIVE");
            statusUpdate.put("newStatus", "CANCELLED");
            statusUpdate.put("timestamp", LocalDateTime.now());
            statusUpdate.put("source", ride.getSource());
            statusUpdate.put("destination", ride.getDestination());
            statusUpdate.put("driverName", ride.getDriver().getName());

            webSocketNotificationController.sendRideStatusUpdate(ride.getDriver().getUsername(), statusUpdate);

            // Send to confirmed passengers
            for (Booking booking : bookings) {
                if (booking.getStatus() == BookingStatus.CONFIRMED) {
                    webSocketNotificationController.sendRideStatusUpdate(booking.getPassenger().getUsername(), statusUpdate);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to broadcast ride status update: " + e.getMessage());
        }
    }

    // ---------------- DELETE RIDE ----------------
    @Transactional
    public void deleteRide(Long rideId, String driverIdentifier) {
        User driver = userRepository.findByEmail(driverIdentifier)
                .orElseGet(() -> userRepository.findByUsername(driverIdentifier)
                        .orElseThrow(() -> new RuntimeException("Driver not found")));
        
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        
        if (!ride.getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("Unauthorized to delete this ride");
        }
        
        // Check if there are any confirmed bookings
        List<Booking> bookings = bookingRepository.findByRideId(rideId);
        boolean hasConfirmedBookings = bookings.stream()
                .anyMatch(b -> b.getStatus() == BookingStatus.CONFIRMED);
        
        if (hasConfirmedBookings) {
            throw new RuntimeException("Cannot delete ride with confirmed bookings. Cancel the ride first.");
        }
        
        rideRepository.delete(ride);
    }

    // ---------------- GET RIDE BOOKINGS ----------------
    public List<Booking> getRideBookings(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        
        return bookingRepository.findByRideId(rideId);
    }

    // ---------------- CALCULATE FARE ----------------
    public Map<String, Object> calculateFare(Long rideId, Double distance, Integer seats) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        
        double distanceKm;
        if (distance != null && distance > 0) {
            distanceKm = distance;
        } else {
            distanceKm = getDistanceKm(ride.getSource(), ride.getDestination());
        }
        
        double totalFare = BASE_FARE + (ride.getPricePerKm() * distanceKm);
        double farePerSeat = totalFare / ride.getTotalSeats();
        double fareForBooking = farePerSeat * seats;
        
        Map<String, Object> fareDetails = new HashMap<>();
        fareDetails.put("distanceKm", distanceKm);
        fareDetails.put("baseFare", BASE_FARE);
        fareDetails.put("pricePerKm", ride.getPricePerKm());
        fareDetails.put("totalRideFare", totalFare);
        fareDetails.put("farePerSeat", farePerSeat);
        fareDetails.put("seatsRequested", seats);
        fareDetails.put("fareForBooking", fareForBooking);
        
        return fareDetails;
    }

    // ---------------- OPENROUTESERVICE API ----------------
    private double getDistanceKm(String source, String destination) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", OPENROUTESERVICE_KEY);
            headers.setContentType(MediaType.APPLICATION_JSON);

            double[] srcCoords = geocode(source);
            double[] destCoords = geocode(destination);

            Map<String, Object> body = new HashMap<>();
            body.put("coordinates", new double[][] { srcCoords, destCoords });

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(OPENROUTESERVICE_URL, entity, String.class);

            logger.info("OpenRouteService response: {}", response.getBody());
            
            JSONObject json = new JSONObject(response.getBody());
            
            // Check if response has the expected structure
            if (json.has("features")) {
                JSONArray features = json.getJSONArray("features");
                if (features.length() > 0) {
                    JSONObject props = features.getJSONObject(0).getJSONObject("properties");
                    double distanceMeters = props.getJSONObject("summary").getDouble("distance");
                    logger.info("✅ Distance calculated via API: {} km", distanceMeters / 1000.0);
                    return distanceMeters / 1000.0; // convert to km
                }
            }
            
            // If no features, try alternative structure
            if (json.has("routes")) {
                JSONArray routes = json.getJSONArray("routes");
                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);
                    if (route.has("summary")) {
                        double distanceMeters = route.getJSONObject("summary").getDouble("distance");
                        logger.info("✅ Distance calculated via API (routes): {} km", distanceMeters / 1000.0);
                        return distanceMeters / 1000.0;
                    }
                }
            }
            
            logger.warn("⚠️ OpenRouteService API returned unexpected format. Using fallback calculation.");
            return calculateFallbackDistance(srcCoords, destCoords);
            
        } catch (Exception e) {
            logger.error("❌ OpenRouteService API error: {}. Using fallback calculation.", e.getMessage());
            double[] srcCoords = geocode(source);
            double[] destCoords = geocode(destination);
            return calculateFallbackDistance(srcCoords, destCoords);
        }
    }
    
    // Haversine formula for distance calculation as fallback
    private double calculateFallbackDistance(double[] coord1, double[] coord2) {
        double lon1 = coord1[0];
        double lat1 = coord1[1];
        double lon2 = coord2[0];
        double lat2 = coord2[1];
        
        final int R = 6371; // Radius of the earth in km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;
        
        logger.info("📍 Fallback distance calculated: {} km", distance);
        return distance;
    }

    private double[] geocode(String location) {
        // Format: [longitude, latitude] for OpenRouteService
        String loc = location.trim().toLowerCase();
        
        // Major Indian cities
        if (loc.contains("chennai")) return new double[]{80.2707, 13.0827};
        if (loc.contains("bangalore") || loc.contains("bengaluru")) return new double[]{77.5946, 12.9716};
        if (loc.contains("mumbai")) return new double[]{72.8777, 19.0760};
        if (loc.contains("delhi")) return new double[]{77.1025, 28.7041};
        if (loc.contains("hyderabad")) return new double[]{78.4867, 17.3850};
        if (loc.contains("pune")) return new double[]{73.8567, 18.5204};
        if (loc.contains("kolkata")) return new double[]{88.3639, 22.5726};
        if (loc.contains("ahmedabad")) return new double[]{72.5714, 23.0225};
        
        // Tamil Nadu cities
        if (loc.contains("tiruttani")) return new double[]{79.6332, 13.1780}; // Tiruttani, Tamil Nadu
        if (loc.contains("tirupati")) return new double[]{79.4192, 13.6288}; // Tirupati, Andhra Pradesh
        if (loc.contains("vellore")) return new double[]{79.1325, 12.9165};
        if (loc.contains("coimbatore")) return new double[]{76.9558, 11.0168};
        if (loc.contains("madurai")) return new double[]{78.1198, 9.9252};
        if (loc.contains("trichy") || loc.contains("tiruchirappalli")) return new double[]{78.6869, 10.7905};
        if (loc.contains("salem")) return new double[]{78.1460, 11.6643};
        if (loc.contains("tirunelveli")) return new double[]{77.6871, 8.7139};
        
        // Andhra Pradesh cities
        if (loc.contains("vijayawada")) return new double[]{80.6480, 16.5062};
        if (loc.contains("visakhapatnam") || loc.contains("vizag")) return new double[]{83.2185, 17.6868};
        if (loc.contains("guntur")) return new double[]{80.4365, 16.3067};
        
        // Karnataka cities
        if (loc.contains("mysore") || loc.contains("mysuru")) return new double[]{76.6394, 12.2958};
        if (loc.contains("mangalore") || loc.contains("mangaluru")) return new double[]{74.8560, 12.9141};
        if (loc.contains("hubli") || loc.contains("hubballi")) return new double[]{75.1240, 15.3647};
        
        // Kerala cities
        if (loc.contains("kochi") || loc.contains("cochin")) return new double[]{76.2673, 9.9312};
        if (loc.contains("trivandrum") || loc.contains("thiruvananthapuram")) return new double[]{76.9366, 8.5241};
        if (loc.contains("kozhikode") || loc.contains("calicut")) return new double[]{75.7804, 11.2588};
        
        // North Indian cities
        if (loc.contains("jaipur")) return new double[]{75.7873, 26.9124};
        if (loc.contains("lucknow")) return new double[]{80.9462, 26.8467};
        if (loc.contains("chandigarh")) return new double[]{76.7794, 30.7333};
        if (loc.contains("indore")) return new double[]{75.8577, 22.7196};
        if (loc.contains("bhopal")) return new double[]{77.4126, 23.2599};
        if (loc.contains("surat")) return new double[]{72.8311, 21.1702};
        
        logger.warn("⚠️ Unknown location '{}', using default coordinates (Bangalore)", location);
        return new double[]{77.5946, 12.9716}; // Default: Bangalore
    }

    // ---------------- SEARCH RIDES WITH SMART ROUTE MATCHING ----------------
    public List<Ride> searchRides(String source, String destination, LocalDate date, Integer seats) {
        List<Ride> allMatches = new ArrayList<>();
        
        if (source != null && destination != null && date != null && seats != null) {
            // Get all available rides for the date with sufficient seats
            List<Ride> availableRides = rideRepository.findByDateAndAvailableSeatsGreaterThanEqual(date, seats);
            
            // Use smart route matching to find all matches
            List<RouteMatchingService.RouteMatch> routeMatches = 
                routeMatchingService.findAllMatches(source, destination, availableRides);
            
            // Convert route matches back to rides and sort by match quality
            allMatches = routeMatches.stream()
                .map(RouteMatchingService.RouteMatch::getRide)
                .collect(Collectors.toList());
            
            logger.info("Found {} route matches for {} -> {} on {}", 
                       allMatches.size(), source, destination, date);
            
            // Log match details for debugging
            for (int i = 0; i < Math.min(5, routeMatches.size()); i++) {
                RouteMatchingService.RouteMatch match = routeMatches.get(i);
                logger.info("Match {}: {} -> {} (Quality: {:.1f}%, Type: {}, Deviation: {:.1f}km)", 
                           i + 1, 
                           match.getRide().getSource(), 
                           match.getRide().getDestination(),
                           match.getMatchQuality(),
                           match.getMatchType(),
                           match.getRouteDeviation());
            }
            
        } else if (source != null && destination != null && seats != null) {
            // Flexible search without date constraint
            allMatches = rideRepository.searchRidesFlexible(source, destination, seats);
        } else {
            // Return all active rides if no specific criteria
            allMatches = rideRepository.findAllActiveRides();
        }
        
        return allMatches;
    }

    /**
     * Enhanced search with route match details for frontend display
     */
    public List<Map<String, Object>> searchRidesWithMatchDetails(String source, String destination, LocalDate date, Integer seats) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        if (source != null && destination != null && date != null && seats != null) {
            List<Ride> availableRides = rideRepository.findByDateAndAvailableSeatsGreaterThanEqual(date, seats);
            List<RouteMatchingService.RouteMatch> routeMatches = 
                routeMatchingService.findAllMatches(source, destination, availableRides);
            
            for (RouteMatchingService.RouteMatch match : routeMatches) {
                Map<String, Object> rideWithMatch = new HashMap<>();
                rideWithMatch.put("ride", match.getRide());
                rideWithMatch.put("matchQuality", match.getMatchQuality());
                rideWithMatch.put("matchType", match.getMatchType());
                rideWithMatch.put("routeDeviation", match.getRouteDeviation());
                
                // Add match quality badge for frontend
                String qualityBadge = getMatchQualityBadge(match.getMatchQuality());
                rideWithMatch.put("qualityBadge", qualityBadge);
                
                results.add(rideWithMatch);
            }
        }
        
        return results;
    }

    private String getMatchQualityBadge(double quality) {
        if (quality >= 95.0) return "PERFECT";
        if (quality >= 85.0) return "EXCELLENT";
        if (quality >= 75.0) return "GOOD";
        if (quality >= 65.0) return "FAIR";
        return "PARTIAL";
    }

    /**
     * Find partial route matches where passenger can join along driver's route
     */
    private List<Ride> findPartialRouteMatches(String source, String destination, LocalDate date, Integer seats) {
        try {
            List<Ride> allRides = rideRepository.findByDateAndAvailableSeatsGreaterThanEqual(date, seats);
            List<Ride> partialMatches = new ArrayList<>();
            
            for (Ride ride : allRides) {
                if (isRouteCompatible(ride.getSource(), ride.getDestination(), source, destination)) {
                    partialMatches.add(ride);
                }
            }
            
            return partialMatches;
        } catch (Exception e) {
            logger.error("Error finding partial route matches: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Smart route matching algorithm
     */
    private boolean isRouteCompatible(String driverSource, String driverDest, String passengerSource, String passengerDest) {
        // Convert to lowercase for comparison
        String ds = driverSource.toLowerCase().trim();
        String dd = driverDest.toLowerCase().trim();
        String ps = passengerSource.toLowerCase().trim();
        String pd = passengerDest.toLowerCase().trim();
        
        // Direct match (already handled by exact search, but double-check)
        if (ds.equals(ps) && dd.equals(pd)) {
            return true;
        }
        
        // Partial source match (same starting city)
        if (ds.contains(ps) || ps.contains(ds)) {
            return true;
        }
        
        // Partial destination match (same ending city)
        if (dd.contains(pd) || pd.contains(dd)) {
            return true;
        }
        
        // Common route patterns (Chennai-Bangalore, Mumbai-Pune, etc.)
        return isCommonRoute(ds, dd, ps, pd);
    }

    /**
     * Check for common route patterns
     */
    private boolean isCommonRoute(String ds, String dd, String ps, String pd) {
        // Major city pairs that are commonly traveled
        String[][] commonRoutes = {
            {"chennai", "bangalore"}, {"bangalore", "chennai"},
            {"mumbai", "pune"}, {"pune", "mumbai"},
            {"delhi", "gurgaon"}, {"gurgaon", "delhi"},
            {"hyderabad", "bangalore"}, {"bangalore", "hyderabad"}
        };
        
        for (String[] route : commonRoutes) {
            if ((ds.contains(route[0]) && dd.contains(route[1])) &&
                (ps.contains(route[0]) && pd.contains(route[1]))) {
                return true;
            }
        }
        
        return false;
    }

    public Ride getRideById(Long rideId) {
        return rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
    }

    // ---------------- GET DRIVER BOOKINGS ----------------
    public List<Booking> getDriverBookings(Long driverId) {
        try {
            logger.info("🔍 Getting bookings for driver ID: {}", driverId);
            
            // Get all rides for this driver
            List<Ride> driverRides = rideRepository.findByDriverId(driverId);
            logger.info("🚗 Found {} rides for driver ID {}", driverRides.size(), driverId);
            
            if (driverRides.isEmpty()) {
                logger.warn("⚠️ No rides found for driver ID {}. Cannot fetch bookings without rides.", driverId);
                return new ArrayList<>();
            }
            
            // Get all bookings for these rides
            List<Booking> allBookings = new ArrayList<>();
            for (Ride ride : driverRides) {
                logger.debug("📝 Checking bookings for ride ID: {}", ride.getId());
                List<Booking> rideBookings = bookingRepository.findByRideIdOrderByBookedAtDesc(ride.getId());
                logger.debug("   Found {} bookings for ride {}", rideBookings.size(), ride.getId());
                allBookings.addAll(rideBookings);
            }
            
            logger.info("✅ Total {} bookings found for driver {}", allBookings.size(), driverId);
            
            if (allBookings.isEmpty()) {
                logger.warn("⚠️ No bookings found for any of driver {}'s rides", driverId);
                logger.warn("   Ride IDs checked: {}", driverRides.stream().map(r -> r.getId()).toList());
            }
            
            return allBookings;
            
        } catch (Exception e) {
            logger.error("❌ Error getting driver bookings: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }



    /**
     * Get all available rides for dashboard
     */
    public List<Ride> getAllAvailableRides() {
        try {
            LocalDate currentDate = LocalDate.now();
            return rideRepository.findAllAvailableRides(currentDate);
        } catch (Exception e) {
            logger.error("Error getting all available rides: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Update ride statuses (delegate to RideStatusService)
     */
    @Autowired
    private RideStatusService rideStatusService;

    public int updateRideStatuses() {
        return rideStatusService.updateRideStatusesManually();
    }

    /**
     * Get global ride statistics
     */
    public RideStatusService.RideStats getGlobalRideStats() {
        return rideStatusService.getRideStats();
    }

    /**
     * Mark booking as completed and trigger review notifications
     */
    @Transactional
    public Booking markBookingAsCompleted(Long bookingId, String userIdentifier) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        User user = userRepository.findByEmail(userIdentifier)
                .orElseGet(() -> userRepository.findByUsername(userIdentifier)
                        .orElseThrow(() -> new RuntimeException("User not found")));

        // Check authorization - both passenger and driver can mark booking as completed
        boolean isPassenger = booking.getPassenger().getId().equals(user.getId());
        boolean isDriver = booking.getRide().getDriver().getId().equals(user.getId());
        
        if (!isPassenger && !isDriver) {
            throw new RuntimeException("Only the passenger or driver can mark this booking as completed");
        }

        // Check current status
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Booking is already marked as completed");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Only confirmed bookings can be marked as completed");
        }

        // Mark as completed
        booking.setStatus(BookingStatus.COMPLETED);
        Booking savedBooking = bookingRepository.save(booking);

        // Send review request notifications
        try {
            notificationService.sendReviewRequestNotification(booking);
        } catch (Exception e) {
            logger.warn("Failed to send review request notifications: " + e.getMessage());
        }

        logger.info("✅ Booking {} marked as completed by {}", bookingId, userIdentifier);
        return savedBooking;
    }

    /**
     * Mark entire ride as completed and all its confirmed bookings
     */
    @Transactional
    public Ride markRideAsCompleted(Long rideId, String driverIdentifier) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        User driver = userRepository.findByEmail(driverIdentifier)
                .orElseGet(() -> userRepository.findByUsername(driverIdentifier)
                        .orElseThrow(() -> new RuntimeException("Driver not found")));

        // Check authorization - only the driver can mark their ride as completed
        if (!ride.getDriver().getId().equals(driver.getId())) {
            throw new RuntimeException("Only the driver can mark this ride as completed");
        }

        // Check current status
        if ("COMPLETED".equals(ride.getStatus())) {
            throw new RuntimeException("Ride is already marked as completed");
        }

        // Mark ride as completed
        ride.setStatus("COMPLETED");
        Ride savedRide = rideRepository.save(ride);

        // Mark all confirmed bookings as completed
        List<Booking> rideBookings = bookingRepository.findByRideIdOrderByBookedAtDesc(rideId);
        int completedCount = 0;
        
        for (Booking booking : rideBookings) {
            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                booking.setStatus(BookingStatus.COMPLETED);
                bookingRepository.save(booking);
                completedCount++;

                // Send review request for each completed booking
                try {
                    notificationService.sendReviewRequestNotification(booking);
                } catch (Exception e) {
                    logger.warn("Failed to send review request for booking {}: {}", 
                        booking.getId(), e.getMessage());
                }
            }
        }

        logger.info("✅ Ride {} marked as completed with {} bookings", rideId, completedCount);
        return savedRide;
    }
}

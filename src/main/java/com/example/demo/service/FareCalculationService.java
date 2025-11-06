package com.example.demo.service;

import com.example.demo.dto.FareRequest;
import com.example.demo.dto.FareResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FareCalculationService {

    private static final Logger logger = LoggerFactory.getLogger(FareCalculationService.class);

    // Fare calculation constants
    private static final double DEFAULT_BASE_FARE = 50.0; // Base fare in rupees
    private static final double DEFAULT_PRICE_PER_KM = 12.0; // Price per kilometer in rupees
    private static final double PLATFORM_COMMISSION = 0.10; // 10% platform commission

    @Autowired
    private GoogleMapsService googleMapsService;

    /**
     * Calculate fare for a ride based on distance and passengers
     * @param fareRequest Request containing source, destination, and passenger count
     * @return FareResponse with detailed fare breakdown
     */
    public FareResponse calculateFare(FareRequest fareRequest) {
        try {
            logger.info("Calculating fare for: {}", fareRequest);

            // Validate input
            if (fareRequest.getSource() == null || fareRequest.getSource().trim().isEmpty()) {
                return FareResponse.error("Source location is required");
            }
            if (fareRequest.getDestination() == null || fareRequest.getDestination().trim().isEmpty()) {
                return FareResponse.error("Destination location is required");
            }
            if (fareRequest.getPassengers() == null || fareRequest.getPassengers() < 1) {
                return FareResponse.error("At least 1 passenger is required");
            }

            // Get distance - prefer free OpenRouteService-like fallback (no paid keys),
            // then fall back to Google if explicitly configured.
            double distance;
            String calculationMethod;

            if (fareRequest.getDistance() != null && fareRequest.getDistance() > 0) {
                distance = fareRequest.getDistance();
                calculationMethod = "PROVIDED";
                logger.info("Using provided distance: {} km", distance);
            } else {
                // First try our internal free method (same strategy as RideService: haversine + road multiplier)
                try {
                    distance = freeDistanceEstimate(
                        fareRequest.getSource().trim(), 
                        fareRequest.getDestination().trim()
                    );
                    calculationMethod = "FREE_OSM_FALLBACK";
                    logger.info("Calculated distance using {}: {} km", calculationMethod, distance);
                } catch (Exception ex) {
                    logger.warn("Free distance estimate failed: {}. Falling back to Google if configured.", ex.getMessage());
                    distance = googleMapsService.calculateDistance(
                        fareRequest.getSource().trim(), 
                        fareRequest.getDestination().trim()
                    );
                    calculationMethod = googleMapsService.isApiConfigured() ? "GOOGLE_MAPS" : "FALLBACK";
                    logger.info("Calculated distance using {}: {} km", calculationMethod, distance);
                }
            }

            // Get price per km - use provided rate or default
            double pricePerKm = fareRequest.getPricePerKm() != null && fareRequest.getPricePerKm() > 0 
                ? fareRequest.getPricePerKm() 
                : DEFAULT_PRICE_PER_KM;

            // Calculate fare components
            double baseFare = DEFAULT_BASE_FARE;
            double distanceFare = pricePerKm * distance;
            double totalFare = baseFare + distanceFare;
            double farePerPassenger = totalFare / fareRequest.getPassengers();

            // Get estimated travel time
            int estimatedTravelTime = (int)Math.ceil((distance / 40.0) * 60.0); // assume 40 km/h average

            // Create fare details
            FareResponse.FareDetails fareDetails = new FareResponse.FareDetails(
                fareRequest.getSource().trim(),
                fareRequest.getDestination().trim(),
                Math.round(distance * 100.0) / 100.0, // Round to 2 decimal places
                fareRequest.getPassengers(),
                baseFare,
                pricePerKm,
                Math.round(distanceFare * 100.0) / 100.0,
                Math.round(totalFare * 100.0) / 100.0,
                Math.round(farePerPassenger * 100.0) / 100.0,
                estimatedTravelTime,
                calculationMethod
            );

            logger.info("Fare calculated successfully: Total=₹{}, Per Passenger=₹{}", 
                totalFare, farePerPassenger);

            return FareResponse.success(fareDetails);

        } catch (Exception e) {
            logger.error("Error calculating fare: {}", e.getMessage(), e);
            return FareResponse.error("Failed to calculate fare: " + e.getMessage());
        }
    }

    // ===== Free distance estimation using Haversine with curated city coordinates (no paid API) =====
    private double freeDistanceEstimate(String origin, String destination) {
        double[] o = geocodeCity(origin);
        double[] d = geocodeCity(destination);
        // Haversine distance in km
        final int R = 6371;
        double lat1 = o[1], lon1 = o[0];
        double lat2 = d[1], lon2 = d[0];
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2) + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLon/2)*Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double straight = R * c;
        // Roads are usually longer than straight-line; inflate by ~16%
        return Math.round((straight * 1.16) * 100.0) / 100.0;
    }

    // Minimal geocoder for major Indian cities (same mapping used elsewhere)
    private double[] geocodeCity(String location) {
        String loc = location.trim().toLowerCase();
        if (loc.contains("chennai")) return new double[]{80.2707, 13.0827};
        if (loc.contains("bangalore") || loc.contains("bengaluru")) return new double[]{77.5946, 12.9716};
        if (loc.contains("mumbai")) return new double[]{72.8777, 19.0760};
        if (loc.contains("delhi")) return new double[]{77.1025, 28.7041};
        if (loc.contains("hyderabad")) return new double[]{78.4867, 17.3850};
        if (loc.contains("pune")) return new double[]{73.8567, 18.5204};
        if (loc.contains("kolkata")) return new double[]{88.3639, 22.5726};
        if (loc.contains("ahmedabad")) return new double[]{72.5714, 23.0225};
        if (loc.contains("tiruttani")) return new double[]{79.6332, 13.1780};
        if (loc.contains("tirupati")) return new double[]{79.4192, 13.6288};
        if (loc.contains("vellore")) return new double[]{79.1325, 12.9165};
        if (loc.contains("coimbatore")) return new double[]{76.9558, 11.0168};
        if (loc.contains("madurai")) return new double[]{78.1198, 9.9252};
        if (loc.contains("trichy") || loc.contains("tiruchirappalli")) return new double[]{78.6869, 10.7905};
        if (loc.contains("salem")) return new double[]{78.1460, 11.6643};
        if (loc.contains("tirunelveli")) return new double[]{77.6871, 8.7139};
        if (loc.contains("vijayawada")) return new double[]{80.6480, 16.5062};
        if (loc.contains("visakhapatnam") || loc.contains("vizag")) return new double[]{83.2185, 17.6868};
        if (loc.contains("guntur")) return new double[]{80.4365, 16.3067};
        if (loc.contains("mysore") || loc.contains("mysuru")) return new double[]{76.6394, 12.2958};
        if (loc.contains("mangalore") || loc.contains("mangaluru")) return new double[]{74.8560, 12.9141};
        if (loc.contains("hubli") || loc.contains("hubballi")) return new double[]{75.1240, 15.3647};
        if (loc.contains("kochi") || loc.contains("cochin")) return new double[]{76.2673, 9.9312};
        if (loc.contains("trivandrum") || loc.contains("thiruvananthapuram")) return new double[]{76.9366, 8.5241};
        if (loc.contains("kozhikode") || loc.contains("calicut")) return new double[]{75.7804, 11.2588};
        if (loc.contains("jaipur")) return new double[]{75.7873, 26.9124};
        if (loc.contains("lucknow")) return new double[]{80.9462, 26.8467};
        if (loc.contains("chandigarh")) return new double[]{76.7794, 30.7333};
        if (loc.contains("indore")) return new double[]{75.8577, 22.7196};
        if (loc.contains("bhopal")) return new double[]{77.4126, 23.2599};
        if (loc.contains("surat")) return new double[]{72.8311, 21.1702};
        return new double[]{77.5946, 12.9716};
    }

    /**
     * Calculate fare for an existing ride with multiple passengers
     * @param rideId ID of the ride
     * @param passengersToAdd Number of passengers to add
     * @param totalSeats Total seats in the ride
     * @param existingFare Current total fare of the ride
     * @return Proportional fare for the new passengers
     */
    public FareResponse calculateProportionalFare(Long rideId, int passengersToAdd, int totalSeats, double existingFare) {
        try {
            logger.info("Calculating proportional fare for ride {}: {} passengers out of {} total seats", 
                rideId, passengersToAdd, totalSeats);

            if (passengersToAdd <= 0) {
                return FareResponse.error("Number of passengers must be greater than 0");
            }
            if (totalSeats <= 0) {
                return FareResponse.error("Total seats must be greater than 0");
            }
            if (existingFare < 0) {
                return FareResponse.error("Existing fare cannot be negative");
            }

            // Calculate fare per seat
            double farePerSeat = existingFare / totalSeats;
            double proportionalFare = farePerSeat * passengersToAdd;

            // Create simplified fare details for proportional calculation
            FareResponse.FareDetails fareDetails = new FareResponse.FareDetails();
            fareDetails.setPassengers(passengersToAdd);
            fareDetails.setTotalFare(Math.round(proportionalFare * 100.0) / 100.0);
            fareDetails.setFarePerPassenger(Math.round(farePerSeat * 100.0) / 100.0);
            fareDetails.setCalculationMethod("PROPORTIONAL");

            logger.info("Proportional fare calculated: ₹{} for {} passengers", proportionalFare, passengersToAdd);

            return FareResponse.success(fareDetails);

        } catch (Exception e) {
            logger.error("Error calculating proportional fare: {}", e.getMessage(), e);
            return FareResponse.error("Failed to calculate proportional fare: " + e.getMessage());
        }
    }

    /**
     * Calculate driver earnings after platform commission
     * @param totalFare Total fare collected from passengers
     * @return Driver earnings after commission deduction
     */
    public double calculateDriverEarnings(double totalFare) {
        if (totalFare <= 0) {
            return 0.0;
        }
        
        double commission = totalFare * PLATFORM_COMMISSION;
        double driverEarnings = totalFare - commission;
        
        logger.info("Driver earnings: ₹{} (Total: ₹{}, Commission: ₹{})", 
            driverEarnings, totalFare, commission);
        
        return Math.round(driverEarnings * 100.0) / 100.0;
    }

    /**
     * Calculate platform commission from total fare
     * @param totalFare Total fare collected from passengers
     * @return Platform commission amount
     */
    public double calculatePlatformCommission(double totalFare) {
        if (totalFare <= 0) {
            return 0.0;
        }
        
        double commission = totalFare * PLATFORM_COMMISSION;
        return Math.round(commission * 100.0) / 100.0;
    }

    /**
     * Get fare estimation for display purposes (without detailed calculation)
     * @param source Source location
     * @param destination Destination location
     * @return Quick fare estimate
     */
    public FareResponse getQuickFareEstimate(String source, String destination) {
        try {
            FareRequest request = new FareRequest(source, destination, 1);
            return calculateFare(request);
        } catch (Exception e) {
            logger.error("Error getting quick fare estimate: {}", e.getMessage());
            return FareResponse.error("Failed to get fare estimate");
        }
    }

    /**
     * Validate fare calculation parameters
     * @param source Source location
     * @param destination Destination location
     * @param passengers Number of passengers
     * @return true if parameters are valid
     */
    public boolean validateFareParameters(String source, String destination, int passengers) {
        return source != null && !source.trim().isEmpty() &&
               destination != null && !destination.trim().isEmpty() &&
               passengers > 0;
    }

    // Getters for constants (useful for frontend display)
    public double getDefaultBaseFare() {
        return DEFAULT_BASE_FARE;
    }

    public double getDefaultPricePerKm() {
        return DEFAULT_PRICE_PER_KM;
    }

    public double getPlatformCommissionRate() {
        return PLATFORM_COMMISSION;
    }
}
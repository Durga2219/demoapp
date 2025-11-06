package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GoogleMapsService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleMapsService.class);

    @Value("${google.maps.api.key}")
    private String apiKey;

    @Value("${google.maps.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public GoogleMapsService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Calculate distance between two locations using Google Maps Distance Matrix API
     * @param origin Source location (e.g., "Chennai, India")
     * @param destination Destination location (e.g., "Bangalore, India")
     * @return Distance in kilometers, or fallback distance if API fails
     */
    public double calculateDistance(String origin, String destination) {
        try {
            // Build API URL with parameters
            String url = String.format("%s?origins=%s&destinations=%s&key=%s&units=metric",
                    apiUrl,
                    origin.replace(" ", "+"),
                    destination.replace(" ", "+"),
                    apiKey);

            logger.info("Calling Google Maps API for distance calculation: {} -> {}", origin, destination);

            // Make API call
            String response = restTemplate.getForObject(url, String.class);
            
            if (response == null) {
                logger.warn("Google Maps API returned null response");
                return getFallbackDistance(origin, destination);
            }

            // Parse JSON response
            JSONObject jsonResponse = new JSONObject(response);
            String status = jsonResponse.getString("status");

            if (!"OK".equals(status)) {
                logger.warn("Google Maps API returned status: {}", status);
                return getFallbackDistance(origin, destination);
            }

            // Extract distance from response
            JSONArray rows = jsonResponse.getJSONArray("rows");
            if (rows.length() == 0) {
                logger.warn("No rows in Google Maps API response");
                return getFallbackDistance(origin, destination);
            }

            JSONArray elements = rows.getJSONObject(0).getJSONArray("elements");
            if (elements.length() == 0) {
                logger.warn("No elements in Google Maps API response");
                return getFallbackDistance(origin, destination);
            }

            JSONObject element = elements.getJSONObject(0);
            String elementStatus = element.getString("status");

            if (!"OK".equals(elementStatus)) {
                logger.warn("Google Maps API element status: {}", elementStatus);
                return getFallbackDistance(origin, destination);
            }

            // Get distance in meters and convert to kilometers
            JSONObject distance = element.getJSONObject("distance");
            double distanceMeters = distance.getDouble("value");
            double distanceKm = distanceMeters / 1000.0;

            logger.info("Distance calculated: {} km", distanceKm);
            return distanceKm;

        } catch (RestClientException e) {
            logger.error("Error calling Google Maps API: {}", e.getMessage());
            return getFallbackDistance(origin, destination);
        } catch (Exception e) {
            logger.error("Error parsing Google Maps API response: {}", e.getMessage());
            return getFallbackDistance(origin, destination);
        }
    }

    /**
     * Fallback distance calculation using Haversine formula with accurate GPS coordinates
     */
    private double getFallbackDistance(String origin, String destination) {
        logger.info("Using Haversine fallback distance calculation for: {} -> {}", origin, destination);

        // Get accurate GPS coordinates
        double[] originCoords = geocodeCity(origin);
        double[] destCoords = geocodeCity(destination);
        
        // Calculate distance using Haversine formula
        double straightLineDistance = calculateHaversineDistance(originCoords, destCoords);
        
        // Apply road distance multiplier (roads are ~16% longer than straight line due to curves)
        double roadDistance = straightLineDistance * 1.16;
        
        logger.info("📍 Straight-line: {} km, Estimated road distance: {} km", 
            Math.round(straightLineDistance * 100.0) / 100.0,
            Math.round(roadDistance * 100.0) / 100.0);
        return roadDistance;
    }
    
    /**
     * Calculate distance between two GPS coordinates using Haversine formula
     */
    private double calculateHaversineDistance(double[] coord1, double[] coord2) {
        double lon1 = coord1[0];
        double lat1 = coord1[1];
        double lon2 = coord2[0];
        double lat2 = coord2[1];
        
        final int R = 6371; // Radius of Earth in km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c; // Distance in km
    }
    
    /**
     * Get GPS coordinates for Indian cities
     * Returns [longitude, latitude]
     */
    private double[] geocodeCity(String location) {
        String loc = location.trim().toLowerCase();
        
        // Major cities
        if (loc.contains("chennai")) return new double[]{80.2707, 13.0827};
        if (loc.contains("bangalore") || loc.contains("bengaluru")) return new double[]{77.5946, 12.9716};
        if (loc.contains("mumbai")) return new double[]{72.8777, 19.0760};
        if (loc.contains("delhi")) return new double[]{77.1025, 28.7041};
        if (loc.contains("hyderabad")) return new double[]{78.4867, 17.3850};
        if (loc.contains("pune")) return new double[]{73.8567, 18.5204};
        if (loc.contains("kolkata")) return new double[]{88.3639, 22.5726};
        if (loc.contains("ahmedabad")) return new double[]{72.5714, 23.0225};
        
        // Tamil Nadu cities
        if (loc.contains("tiruttani")) return new double[]{79.6332, 13.1780};
        if (loc.contains("tirupati")) return new double[]{79.4192, 13.6288};
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
        
        logger.warn("⚠️ Unknown location '{}', using Bangalore coordinates", location);
        return new double[]{77.5946, 12.9716}; // Default: Bangalore
    }

    /**
     * Get estimated travel time between two locations
     * @param origin Source location
     * @param destination Destination location
     * @return Estimated travel time in minutes
     */
    public int getEstimatedTravelTime(String origin, String destination) {
        double distance = calculateDistance(origin, destination);
        // Assume average speed of 60 km/h for travel time estimation
        double timeHours = distance / 60.0;
        return (int) Math.ceil(timeHours * 60); // Convert to minutes
    }

    /**
     * Check if Google Maps API is configured and available
     * @return true if API key is configured
     */
    public boolean isApiConfigured() {
        return apiKey != null && !apiKey.isEmpty() && !"YOUR_GOOGLE_MAPS_API_KEY_HERE".equals(apiKey);
    }
}
package com.example.demo.service;

import com.example.demo.entity.Ride;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RouteMatchingService {

    @Autowired
    private GoogleMapsService googleMapsService;

    /**
     * Find direct route matches where source and destination are exactly the same
     */
    public List<RouteMatch> findDirectMatches(String source, String destination, List<Ride> availableRides) {
        return availableRides.stream()
                .filter(ride -> isDirectMatch(ride, source, destination))
                .map(ride -> new RouteMatch(ride, 100.0, 0.0, "DIRECT"))
                .collect(Collectors.toList());
    }

    /**
     * Find partial route matches where the passenger's route overlaps with existing rides
     */
    public List<RouteMatch> findPartialMatches(String source, String destination, List<Ride> availableRides) {
        List<RouteMatch> partialMatches = new ArrayList<>();

        for (Ride ride : availableRides) {
            if (!isDirectMatch(ride, source, destination)) {
                RouteMatch match = calculatePartialMatch(ride, source, destination);
                if (match != null && match.getMatchQuality() >= 60.0) { // Minimum 60% match quality
                    partialMatches.add(match);
                }
            }
        }

        // Sort by match quality (highest first)
        partialMatches.sort((a, b) -> Double.compare(b.getMatchQuality(), a.getMatchQuality()));
        return partialMatches;
    }

    /**
     * Find all matches (direct + partial) and rank them by quality
     */
    public List<RouteMatch> findAllMatches(String source, String destination, List<Ride> availableRides) {
        List<RouteMatch> allMatches = new ArrayList<>();
        
        // Add direct matches first (highest priority)
        allMatches.addAll(findDirectMatches(source, destination, availableRides));
        
        // Add partial matches
        allMatches.addAll(findPartialMatches(source, destination, availableRides));
        
        // Sort by match quality and deviation
        allMatches.sort((a, b) -> {
            int qualityCompare = Double.compare(b.getMatchQuality(), a.getMatchQuality());
            if (qualityCompare == 0) {
                return Double.compare(a.getRouteDeviation(), b.getRouteDeviation());
            }
            return qualityCompare;
        });
        
        return allMatches;
    }

    private boolean isDirectMatch(Ride ride, String source, String destination) {
        return normalizeLocation(ride.getSource()).equals(normalizeLocation(source)) &&
               normalizeLocation(ride.getDestination()).equals(normalizeLocation(destination));
    }

    private RouteMatch calculatePartialMatch(Ride ride, String passengerSource, String passengerDestination) {
        try {
            // Calculate distances for route analysis
            double rideDistance = googleMapsService.calculateDistance(ride.getSource(), ride.getDestination());
            double passengerDistance = googleMapsService.calculateDistance(passengerSource, passengerDestination);
            
            // Check if passenger source is near ride route
            double sourceToRideSource = googleMapsService.calculateDistance(passengerSource, ride.getSource());
            double sourceToRideDestination = googleMapsService.calculateDistance(passengerSource, ride.getDestination());
            
            // Check if passenger destination is near ride route
            double destinationToRideSource = googleMapsService.calculateDistance(passengerDestination, ride.getSource());
            double destinationToRideDestination = googleMapsService.calculateDistance(passengerDestination, ride.getDestination());
            
            // Calculate route overlap and deviation
            RouteAnalysis analysis = analyzeRouteOverlap(
                ride.getSource(), ride.getDestination(),
                passengerSource, passengerDestination,
                rideDistance, passengerDistance,
                sourceToRideSource, sourceToRideDestination,
                destinationToRideSource, destinationToRideDestination
            );
            
            if (analysis.matchQuality >= 60.0) {
                return new RouteMatch(ride, analysis.matchQuality, analysis.deviation, analysis.matchType);
            }
            
        } catch (Exception e) {
            // Fallback to basic distance calculation
            return calculateFallbackMatch(ride, passengerSource, passengerDestination);
        }
        
        return null;
    }

    private RouteAnalysis analyzeRouteOverlap(String rideSource, String rideDestination,
                                            String passengerSource, String passengerDestination,
                                            double rideDistance, double passengerDistance,
                                            double sourceToRideSource, double sourceToRideDestination,
                                            double destinationToRideSource, double destinationToRideDestination) {
        
        RouteAnalysis analysis = new RouteAnalysis();
        
        // Scenario 1: Passenger route is subset of ride route
        if (sourceToRideSource <= 5.0 && destinationToRideDestination <= 5.0) {
            analysis.matchQuality = 95.0;
            analysis.deviation = Math.max(sourceToRideSource, destinationToRideDestination);
            analysis.matchType = "SUBSET";
            return analysis;
        }
        
        // Scenario 2: Ride route is subset of passenger route
        if (destinationToRideSource <= 5.0 && sourceToRideDestination <= 5.0) {
            analysis.matchQuality = 85.0;
            analysis.deviation = Math.max(destinationToRideSource, sourceToRideDestination);
            analysis.matchType = "SUPERSET";
            return analysis;
        }
        
        // Scenario 3: Partial overlap - passenger joins midway
        if (sourceToRideSource <= 10.0 && destinationToRideDestination <= 5.0) {
            double overlapPercentage = calculateOverlapPercentage(rideDistance, passengerDistance, sourceToRideSource);
            analysis.matchQuality = Math.max(60.0, 80.0 * overlapPercentage);
            analysis.deviation = sourceToRideSource + destinationToRideDestination;
            analysis.matchType = "PARTIAL_JOIN";
            return analysis;
        }
        
        // Scenario 4: Partial overlap - passenger leaves early
        if (sourceToRideSource <= 5.0 && destinationToRideSource <= 10.0) {
            double overlapPercentage = calculateOverlapPercentage(rideDistance, passengerDistance, destinationToRideSource);
            analysis.matchQuality = Math.max(60.0, 75.0 * overlapPercentage);
            analysis.deviation = sourceToRideSource + destinationToRideSource;
            analysis.matchType = "PARTIAL_LEAVE";
            return analysis;
        }
        
        // No good match found
        analysis.matchQuality = 0.0;
        analysis.deviation = Double.MAX_VALUE;
        analysis.matchType = "NO_MATCH";
        return analysis;
    }

    private double calculateOverlapPercentage(double rideDistance, double passengerDistance, double deviation) {
        double effectiveOverlap = Math.min(rideDistance, passengerDistance) - deviation;
        return Math.max(0.0, effectiveOverlap / Math.max(rideDistance, passengerDistance));
    }

    private RouteMatch calculateFallbackMatch(Ride ride, String passengerSource, String passengerDestination) {
        // Simple fallback based on string similarity and basic distance estimation
        double sourceMatch = calculateLocationSimilarity(ride.getSource(), passengerSource);
        double destinationMatch = calculateLocationSimilarity(ride.getDestination(), passengerDestination);
        
        double matchQuality = (sourceMatch + destinationMatch) / 2.0;
        
        if (matchQuality >= 60.0) {
            return new RouteMatch(ride, matchQuality, 0.0, "FALLBACK");
        }
        
        return null;
    }

    private double calculateLocationSimilarity(String location1, String location2) {
        String norm1 = normalizeLocation(location1);
        String norm2 = normalizeLocation(location2);
        
        if (norm1.equals(norm2)) {
            return 100.0;
        }
        
        // Check if one location contains the other
        if (norm1.contains(norm2) || norm2.contains(norm1)) {
            return 80.0;
        }
        
        // Basic string similarity (can be enhanced with more sophisticated algorithms)
        return calculateStringSimilarity(norm1, norm2);
    }

    private String normalizeLocation(String location) {
        return location.toLowerCase()
                .trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[^a-z0-9\\s]", "");
    }

    private double calculateStringSimilarity(String s1, String s2) {
        // Simple Levenshtein distance-based similarity
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 100.0;
        
        int distance = levenshteinDistance(s1, s2);
        return Math.max(0.0, (1.0 - (double) distance / maxLength) * 100.0);
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }

    // Inner classes for route analysis
    private static class RouteAnalysis {
        double matchQuality;
        double deviation;
        String matchType;
    }

    // Route match result class
    public static class RouteMatch {
        private final Ride ride;
        private final double matchQuality;
        private final double routeDeviation;
        private final String matchType;

        public RouteMatch(Ride ride, double matchQuality, double routeDeviation, String matchType) {
            this.ride = ride;
            this.matchQuality = matchQuality;
            this.routeDeviation = routeDeviation;
            this.matchType = matchType;
        }

        // Getters
        public Ride getRide() { return ride; }
        public double getMatchQuality() { return matchQuality; }
        public double getRouteDeviation() { return routeDeviation; }
        public String getMatchType() { return matchType; }
    }
}
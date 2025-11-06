package com.example.demo.util;

import com.example.demo.service.GoogleMapsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class RouteAnalyzer {

    @Autowired
    private GoogleMapsService googleMapsService;

    /**
     * Calculate route similarity between two routes
     */
    public double calculateRouteSimilarity(String route1Source, String route1Destination,
                                         String route2Source, String route2Destination) {
        try {
            // Calculate distances
            double route1Distance = googleMapsService.calculateDistance(route1Source, route1Destination);
            double route2Distance = googleMapsService.calculateDistance(route2Source, route2Destination);
            
            // Calculate cross distances
            double sourceToSource = googleMapsService.calculateDistance(route1Source, route2Source);
            double destToDest = googleMapsService.calculateDistance(route1Destination, route2Destination);
            double sourceToDest = googleMapsService.calculateDistance(route1Source, route2Destination);
            double destToSource = googleMapsService.calculateDistance(route1Destination, route2Source);
            
            // Analyze route relationship
            return analyzeRouteRelationship(route1Distance, route2Distance, 
                                          sourceToSource, destToDest, sourceToDest, destToSource);
            
        } catch (Exception e) {
            // Fallback to string-based similarity
            return calculateStringBasedSimilarity(route1Source, route1Destination, 
                                                route2Source, route2Destination);
        }
    }

    /**
     * Rank routes based on multiple criteria
     */
    public List<RouteRanking> rankRoutes(List<RouteCandidate> candidates, String passengerSource, String passengerDestination) {
        List<RouteRanking> rankings = new ArrayList<>();
        
        for (RouteCandidate candidate : candidates) {
            double similarity = calculateRouteSimilarity(passengerSource, passengerDestination,
                                                       candidate.getSource(), candidate.getDestination());
            
            double convenience = calculateConvenienceScore(candidate, passengerSource, passengerDestination);
            double efficiency = calculateEfficiencyScore(candidate);
            
            // Weighted scoring: similarity (40%), convenience (35%), efficiency (25%)
            double totalScore = (similarity * 0.4) + (convenience * 0.35) + (efficiency * 0.25);
            
            rankings.add(new RouteRanking(candidate, totalScore, similarity, convenience, efficiency));
        }
        
        // Sort by total score (highest first)
        rankings.sort((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()));
        
        return rankings;
    }

    /**
     * Calculate the optimal pickup and drop points for a partial match
     */
    public OptimalPoints calculateOptimalPoints(String rideSource, String rideDestination,
                                              String passengerSource, String passengerDestination) {
        try {
            // Find the best pickup point (closest to passenger source along the ride route)
            String optimalPickup = findOptimalPickupPoint(rideSource, rideDestination, passengerSource);
            
            // Find the best drop point (closest to passenger destination along the ride route)
            String optimalDrop = findOptimalDropPoint(rideSource, rideDestination, passengerDestination);
            
            // Calculate additional distances
            double passengerToPickup = googleMapsService.calculateDistance(passengerSource, optimalPickup);
            double dropToDestination = googleMapsService.calculateDistance(optimalDrop, passengerDestination);
            
            return new OptimalPoints(optimalPickup, optimalDrop, passengerToPickup, dropToDestination);
            
        } catch (Exception e) {
            // Fallback to ride source/destination
            return new OptimalPoints(rideSource, rideDestination, 0.0, 0.0);
        }
    }

    private double analyzeRouteRelationship(double route1Distance, double route2Distance,
                                          double sourceToSource, double destToDest,
                                          double sourceToDest, double destToSource) {
        
        // Perfect match - same start and end points
        if (sourceToSource <= 2.0 && destToDest <= 2.0) {
            return 100.0;
        }
        
        // Reverse route match
        if (sourceToDest <= 2.0 && destToSource <= 2.0) {
            return 95.0;
        }
        
        // Subset/superset relationship
        double avgDistance = (route1Distance + route2Distance) / 2.0;
        double startEndDeviation = (sourceToSource + destToDest) / 2.0;
        
        if (startEndDeviation <= avgDistance * 0.1) { // Within 10% of average distance
            return Math.max(80.0, 100.0 - (startEndDeviation / avgDistance * 100.0));
        }
        
        // Partial overlap calculation
        double minDeviation = Math.min(Math.min(sourceToSource, destToDest), 
                                     Math.min(sourceToDest, destToSource));
        
        if (minDeviation <= avgDistance * 0.3) { // Within 30% of average distance
            return Math.max(60.0, 80.0 - (minDeviation / avgDistance * 100.0));
        }
        
        // Low similarity
        return Math.max(0.0, 60.0 - (minDeviation / avgDistance * 100.0));
    }

    private double calculateStringBasedSimilarity(String route1Source, String route1Destination,
                                                String route2Source, String route2Destination) {
        double sourceSimilarity = calculateLocationSimilarity(route1Source, route2Source);
        double destSimilarity = calculateLocationSimilarity(route1Destination, route2Destination);
        
        return (sourceSimilarity + destSimilarity) / 2.0;
    }

    private double calculateLocationSimilarity(String location1, String location2) {
        String norm1 = normalizeLocation(location1);
        String norm2 = normalizeLocation(location2);
        
        if (norm1.equals(norm2)) return 100.0;
        if (norm1.contains(norm2) || norm2.contains(norm1)) return 80.0;
        
        // Calculate string similarity
        int maxLength = Math.max(norm1.length(), norm2.length());
        if (maxLength == 0) return 100.0;
        
        int distance = levenshteinDistance(norm1, norm2);
        return Math.max(0.0, (1.0 - (double) distance / maxLength) * 100.0);
    }

    private double calculateConvenienceScore(RouteCandidate candidate, String passengerSource, String passengerDestination) {
        // Factors: time of day preference, available seats, vehicle comfort
        double score = 70.0; // Base score
        
        // Available seats bonus
        if (candidate.getAvailableSeats() >= 2) {
            score += 15.0;
        } else if (candidate.getAvailableSeats() == 1) {
            score += 5.0;
        }
        
        // Vehicle type bonus (if available)
        if (candidate.getVehicleModel() != null) {
            if (candidate.getVehicleModel().toLowerCase().contains("suv") || 
                candidate.getVehicleModel().toLowerCase().contains("innova")) {
                score += 10.0;
            } else if (candidate.getVehicleModel().toLowerCase().contains("sedan")) {
                score += 5.0;
            }
        }
        
        return Math.min(100.0, score);
    }

    private double calculateEfficiencyScore(RouteCandidate candidate) {
        // Factors: fare per km, total fare, estimated time
        double score = 70.0; // Base score
        
        // Fare efficiency (lower fare per km is better)
        if (candidate.getPricePerKm() <= 5.0) {
            score += 20.0;
        } else if (candidate.getPricePerKm() <= 8.0) {
            score += 10.0;
        } else if (candidate.getPricePerKm() <= 12.0) {
            score += 5.0;
        }
        
        // Total fare consideration
        if (candidate.getFare() <= 300.0) {
            score += 10.0;
        } else if (candidate.getFare() <= 500.0) {
            score += 5.0;
        }
        
        return Math.min(100.0, score);
    }

    private String findOptimalPickupPoint(String rideSource, String rideDestination, String passengerSource) {
        // For now, return ride source as pickup point
        // In a real implementation, this would calculate intermediate points along the route
        return rideSource;
    }

    private String findOptimalDropPoint(String rideSource, String rideDestination, String passengerDestination) {
        // For now, return ride destination as drop point
        // In a real implementation, this would calculate intermediate points along the route
        return rideDestination;
    }

    private String normalizeLocation(String location) {
        return location.toLowerCase().trim().replaceAll("\\s+", " ").replaceAll("[^a-z0-9\\s]", "");
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;
        
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

    // Data classes
    public static class RouteCandidate {
        private String source;
        private String destination;
        private int availableSeats;
        private String vehicleModel;
        private double pricePerKm;
        private double fare;

        // Constructor
        public RouteCandidate(String source, String destination, int availableSeats, 
                            String vehicleModel, double pricePerKm, double fare) {
            this.source = source;
            this.destination = destination;
            this.availableSeats = availableSeats;
            this.vehicleModel = vehicleModel;
            this.pricePerKm = pricePerKm;
            this.fare = fare;
        }

        // Getters
        public String getSource() { return source; }
        public String getDestination() { return destination; }
        public int getAvailableSeats() { return availableSeats; }
        public String getVehicleModel() { return vehicleModel; }
        public double getPricePerKm() { return pricePerKm; }
        public double getFare() { return fare; }
    }

    public static class RouteRanking {
        private RouteCandidate candidate;
        private double totalScore;
        private double similarityScore;
        private double convenienceScore;
        private double efficiencyScore;

        public RouteRanking(RouteCandidate candidate, double totalScore, 
                          double similarityScore, double convenienceScore, double efficiencyScore) {
            this.candidate = candidate;
            this.totalScore = totalScore;
            this.similarityScore = similarityScore;
            this.convenienceScore = convenienceScore;
            this.efficiencyScore = efficiencyScore;
        }

        // Getters
        public RouteCandidate getCandidate() { return candidate; }
        public double getTotalScore() { return totalScore; }
        public double getSimilarityScore() { return similarityScore; }
        public double getConvenienceScore() { return convenienceScore; }
        public double getEfficiencyScore() { return efficiencyScore; }
    }

    public static class OptimalPoints {
        private String pickupPoint;
        private String dropPoint;
        private double distanceToPickup;
        private double distanceFromDrop;

        public OptimalPoints(String pickupPoint, String dropPoint, 
                           double distanceToPickup, double distanceFromDrop) {
            this.pickupPoint = pickupPoint;
            this.dropPoint = dropPoint;
            this.distanceToPickup = distanceToPickup;
            this.distanceFromDrop = distanceFromDrop;
        }

        // Getters
        public String getPickupPoint() { return pickupPoint; }
        public String getDropPoint() { return dropPoint; }
        public double getDistanceToPickup() { return distanceToPickup; }
        public double getDistanceFromDrop() { return distanceFromDrop; }
    }
}
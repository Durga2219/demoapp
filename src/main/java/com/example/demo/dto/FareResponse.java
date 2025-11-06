package com.example.demo.dto;

public class FareResponse {

    private boolean success;
    private String message;
    private FareDetails fareDetails;

    // Default constructor
    public FareResponse() {}

    // Constructor for success response
    public FareResponse(boolean success, String message, FareDetails fareDetails) {
        this.success = success;
        this.message = message;
        this.fareDetails = fareDetails;
    }

    // Constructor for error response
    public FareResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.fareDetails = null;
    }

    // Static factory methods for common responses
    public static FareResponse success(FareDetails fareDetails) {
        return new FareResponse(true, "Fare calculated successfully", fareDetails);
    }

    public static FareResponse error(String message) {
        return new FareResponse(false, message);
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public FareDetails getFareDetails() {
        return fareDetails;
    }

    public void setFareDetails(FareDetails fareDetails) {
        this.fareDetails = fareDetails;
    }

    // Inner class for fare breakdown details
    public static class FareDetails {
        private String source;
        private String destination;
        private Double distance; // in kilometers
        private Integer passengers;
        private Double baseFare;
        private Double pricePerKm;
        private Double distanceFare; // pricePerKm * distance
        private Double totalFare; // baseFare + distanceFare
        private Double farePerPassenger; // totalFare / passengers
        private Integer estimatedTravelTime; // in minutes
        private String calculationMethod; // "GOOGLE_MAPS" or "FALLBACK"

        // Default constructor
        public FareDetails() {}

        // Full constructor
        public FareDetails(String source, String destination, Double distance, Integer passengers,
                          Double baseFare, Double pricePerKm, Double distanceFare, Double totalFare,
                          Double farePerPassenger, Integer estimatedTravelTime, String calculationMethod) {
            this.source = source;
            this.destination = destination;
            this.distance = distance;
            this.passengers = passengers;
            this.baseFare = baseFare;
            this.pricePerKm = pricePerKm;
            this.distanceFare = distanceFare;
            this.totalFare = totalFare;
            this.farePerPassenger = farePerPassenger;
            this.estimatedTravelTime = estimatedTravelTime;
            this.calculationMethod = calculationMethod;
        }

        // Getters and Setters
        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public Double getDistance() {
            return distance;
        }

        public void setDistance(Double distance) {
            this.distance = distance;
        }

        public Integer getPassengers() {
            return passengers;
        }

        public void setPassengers(Integer passengers) {
            this.passengers = passengers;
        }

        public Double getBaseFare() {
            return baseFare;
        }

        public void setBaseFare(Double baseFare) {
            this.baseFare = baseFare;
        }

        public Double getPricePerKm() {
            return pricePerKm;
        }

        public void setPricePerKm(Double pricePerKm) {
            this.pricePerKm = pricePerKm;
        }

        public Double getDistanceFare() {
            return distanceFare;
        }

        public void setDistanceFare(Double distanceFare) {
            this.distanceFare = distanceFare;
        }

        public Double getTotalFare() {
            return totalFare;
        }

        public void setTotalFare(Double totalFare) {
            this.totalFare = totalFare;
        }

        public Double getFarePerPassenger() {
            return farePerPassenger;
        }

        public void setFarePerPassenger(Double farePerPassenger) {
            this.farePerPassenger = farePerPassenger;
        }

        public Integer getEstimatedTravelTime() {
            return estimatedTravelTime;
        }

        public void setEstimatedTravelTime(Integer estimatedTravelTime) {
            this.estimatedTravelTime = estimatedTravelTime;
        }

        public String getCalculationMethod() {
            return calculationMethod;
        }

        public void setCalculationMethod(String calculationMethod) {
            this.calculationMethod = calculationMethod;
        }

        @Override
        public String toString() {
            return "FareDetails{" +
                    "source='" + source + '\'' +
                    ", destination='" + destination + '\'' +
                    ", distance=" + distance +
                    ", passengers=" + passengers +
                    ", baseFare=" + baseFare +
                    ", pricePerKm=" + pricePerKm +
                    ", distanceFare=" + distanceFare +
                    ", totalFare=" + totalFare +
                    ", farePerPassenger=" + farePerPassenger +
                    ", estimatedTravelTime=" + estimatedTravelTime +
                    ", calculationMethod='" + calculationMethod + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "FareResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", fareDetails=" + fareDetails +
                '}';
    }
}
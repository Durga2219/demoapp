package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

public class FareRequest {

    @NotBlank(message = "Source location is required")
    private String source;

    @NotBlank(message = "Destination location is required")
    private String destination;

    @NotNull(message = "Number of passengers is required")
    @Min(value = 1, message = "At least 1 passenger is required")
    private Integer passengers;

    @Min(value = 0, message = "Distance must be positive")
    private Double distance; // Optional - if provided, skip Google Maps API call

    private Double pricePerKm; // Optional - if not provided, use default rate

    // Default constructor
    public FareRequest() {}

    // Constructor with required fields
    public FareRequest(String source, String destination, Integer passengers) {
        this.source = source;
        this.destination = destination;
        this.passengers = passengers;
    }

    // Full constructor
    public FareRequest(String source, String destination, Integer passengers, Double distance, Double pricePerKm) {
        this.source = source;
        this.destination = destination;
        this.passengers = passengers;
        this.distance = distance;
        this.pricePerKm = pricePerKm;
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

    public Integer getPassengers() {
        return passengers;
    }

    public void setPassengers(Integer passengers) {
        this.passengers = passengers;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public Double getPricePerKm() {
        return pricePerKm;
    }

    public void setPricePerKm(Double pricePerKm) {
        this.pricePerKm = pricePerKm;
    }

    @Override
    public String toString() {
        return "FareRequest{" +
                "source='" + source + '\'' +
                ", destination='" + destination + '\'' +
                ", passengers=" + passengers +
                ", distance=" + distance +
                ", pricePerKm=" + pricePerKm +
                '}';
    }
}
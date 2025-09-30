package com.example.demo.dto;

public class BookingRequest {
    private Integer seatsBooked;
    private String pickupLocation;
    private String dropLocation;
    private Double distance;

    // Constructors
    public BookingRequest() {}

    public BookingRequest(Integer seatsBooked, String pickupLocation,
                          String dropLocation, Double distance) {
        this.seatsBooked = seatsBooked;
        this.pickupLocation = pickupLocation;
        this.dropLocation = dropLocation;
        this.distance = distance;
    }

    // Getters and Setters
    public Integer getSeatsBooked() { return seatsBooked; }
    public void setSeatsBooked(Integer seatsBooked) { this.seatsBooked = seatsBooked; }

    public String getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }

    public String getDropLocation() { return dropLocation; }
    public void setDropLocation(String dropLocation) { this.dropLocation = dropLocation; }

    public Double getDistance() { return distance; }
    public void setDistance(Double distance) { this.distance = distance; }
}

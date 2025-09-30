package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class RideRequest {
    private String source;
    private String destination;
    private LocalDate date;
    private LocalTime time;
    private Integer availableSeats;
    private Integer totalSeats;
    private String vehicleModel;
    private String vehiclePlate;
    private Double pricePerKm;

    // Constructors
    public RideRequest() {}

    public RideRequest(String source, String destination, LocalDate date,
                       LocalTime time, Integer availableSeats, Integer totalSeats,
                       String vehicleModel, String vehiclePlate, Double pricePerKm) {
        this.source = source;
        this.destination = destination;
        this.date = date;
        this.time = time;
        this.availableSeats = availableSeats;
        this.totalSeats = totalSeats;
        this.vehicleModel = vehicleModel;
        this.vehiclePlate = vehiclePlate;
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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public Integer getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(Integer availableSeats) {
        this.availableSeats = availableSeats;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public String getVehiclePlate() {
        return vehiclePlate;
    }

    public void setVehiclePlate(String vehiclePlate) {
        this.vehiclePlate = vehiclePlate;
    }

    public Double getPricePerKm() {
        return pricePerKm;
    }

    public void setPricePerKm(Double pricePerKm) {
        this.pricePerKm = pricePerKm;
    }
}
package com.example.demo.service;

import com.example.demo.dto.RideRequest;
import com.example.demo.dto.BookingRequest;
import com.example.demo.entity.Ride;
import com.example.demo.entity.Booking;
import com.example.demo.entity.User;
import com.example.demo.repository.RideRepository;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class RideService {

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    // Driver posts a new ride
    @Transactional
    public Ride postRide(RideRequest request, String driverEmail) {
        User driver = userRepository.findByEmail(driverEmail)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        if (!driver.getRole().name().equals("DRIVER")) {
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
        ride.setPricePerKm(request.getPricePerKm() != null ? request.getPricePerKm() : 10.0);
        ride.setStatus("ACTIVE");

        return rideRepository.save(ride);
    }

    // Search rides by source, destination, and date
    public List<Ride> searchRides(String source, String destination, LocalDate date) {
        if (source != null && destination != null && date != null) {
            return rideRepository.findBySourceAndDestinationAndDateAndStatus(
                    source, destination, date, "ACTIVE");
        } else if (source != null && destination != null) {
            return rideRepository.findBySourceAndDestinationAndStatus(
                    source, destination, "ACTIVE");
        } else {
            return rideRepository.findByStatus("ACTIVE");
        }
    }

    // Get ride details by ID
    public Ride getRideById(Long rideId) {
        return rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
    }

    // Book a ride
    @Transactional
    public Booking bookRide(Long rideId, BookingRequest request, String passengerEmail) {
        User passenger = userRepository.findByEmail(passengerEmail)
                .orElseThrow(() -> new RuntimeException("Passenger not found"));

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        // Validation
        if (ride.getAvailableSeats() < request.getSeatsBooked()) {
            throw new RuntimeException("Not enough seats available");
        }

        if (ride.getDriver().getId().equals(passenger.getId())) {
            throw new RuntimeException("Driver cannot book their own ride");
        }

        // Create booking
        Booking booking = new Booking();
        booking.setRide(ride);
        booking.setPassenger(passenger);
        booking.setSeatsBooked(request.getSeatsBooked());
        booking.setPickupLocation(request.getPickupLocation());
        booking.setDropLocation(request.getDropLocation());

        // Calculate fare (basic calculation)
        double distance = request.getDistance() != null ? request.getDistance() : 50.0;
        double fare = distance * ride.getPricePerKm() * request.getSeatsBooked();
        booking.setFare(fare);
        booking.setStatus("CONFIRMED");

        // Update available seats
        ride.setAvailableSeats(ride.getAvailableSeats() - request.getSeatsBooked());
        if (ride.getAvailableSeats() == 0) {
            ride.setStatus("FULL");
        }
        rideRepository.save(ride);

        return bookingRepository.save(booking);
    }

    // Get driver's posted rides
    public List<Ride> getDriverRides(String driverEmail) {
        User driver = userRepository.findByEmail(driverEmail)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        return rideRepository.findByDriverId(driver.getId());
    }

    // Get passenger's bookings
    public List<Booking> getPassengerBookings(String passengerEmail) {
        User passenger = userRepository.findByEmail(passengerEmail)
                .orElseThrow(() -> new RuntimeException("Passenger not found"));
        return bookingRepository.findByPassengerId(passenger.getId());
    }

    // Cancel booking
    @Transactional
    public void cancelBooking(Long bookingId, String userEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!booking.getPassenger().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to cancel this booking");
        }

        // Restore seats
        Ride ride = booking.getRide();
        ride.setAvailableSeats(ride.getAvailableSeats() + booking.getSeatsBooked());
        ride.setStatus("ACTIVE");
        rideRepository.save(ride);


        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);
    }
}
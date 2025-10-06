package com.example.demo.service;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RideService {

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    // ---------------- POST RIDE ----------------
    @Transactional
    public Ride postRide(RideRequest request, String driverIdentifier) {
        // Find driver by email or username
        User driver = userRepository.findByEmail(driverIdentifier)
                .orElseGet(() -> userRepository.findByUsername(driverIdentifier)
                        .orElseThrow(() -> new RuntimeException("Driver not found")));

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
        ride.setPricePerKm(request.getPricePerKm() != null ? request.getPricePerKm() : 10.0);
        ride.setStatus("ACTIVE");

        return rideRepository.save(ride);
    }

    // ---------------- SEARCH RIDES ----------------
    public List<Ride> searchRides(String source, String destination, LocalDate date, Integer seats) {
        if (source != null && destination != null && date != null && seats != null) {
            return rideRepository.searchRides(source, destination, date, seats);
        } else if (source != null && destination != null && seats != null) {
            return rideRepository.searchRidesFlexible(source, destination, seats);
        } else {
            return rideRepository.findAllActiveRides();
        }
    }

    // ---------------- GET RIDE BY ID ----------------
    public Ride getRideById(Long rideId) {
        return rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
    }

    // ---------------- BOOK RIDE ----------------
    @Transactional
    public Booking bookRide(Long rideId, BookingRequest request, String passengerIdentifier) {
        // Find passenger by email or username
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
        booking.setPickupLocation(request.getPickupLocation());
        booking.setDropLocation(request.getDropLocation());

        double fare = (request.getDistance() != null ? request.getDistance() : 50.0)
                * ride.getPricePerKm()
                * request.getSeatsBooked();
        booking.setFare(fare);
        booking.setStatus(BookingStatus.CONFIRMED);

        ride.setAvailableSeats(ride.getAvailableSeats() - request.getSeatsBooked());
        if (ride.getAvailableSeats() == 0) ride.setStatus("FULL");
        rideRepository.save(ride);

        return bookingRepository.save(booking);
    }

    // ---------------- DRIVER RIDES ----------------
    public List<Ride> getDriverRides(String driverIdentifier) {
        User driver = userRepository.findByEmail(driverIdentifier)
                .orElseGet(() -> userRepository.findByUsername(driverIdentifier)
                        .orElseThrow(() -> new RuntimeException("Driver not found")));
        return rideRepository.findByDriverId(driver.getId());
    }

    // ---------------- PASSENGER BOOKINGS ----------------
    public List<Booking> getPassengerBookings(String passengerIdentifier) {
        User passenger = userRepository.findByEmail(passengerIdentifier)
                .orElseGet(() -> userRepository.findByUsername(passengerIdentifier)
                        .orElseThrow(() -> new RuntimeException("Passenger not found")));
        return bookingRepository.findByPassengerId(passenger.getId());
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

        Ride ride = booking.getRide();
        ride.setAvailableSeats(ride.getAvailableSeats() + booking.getSeatsBooked());
        ride.setStatus("ACTIVE");
        rideRepository.save(ride);

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);
    }
}

package car_polling_project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    @Autowired
    private RideRepository rideRepository;
    @Autowired
    private customerRepository customerRepository;
    @Autowired
    private PassengerRepository passengerRepository;
      @Autowired
      private BookingRepository bookingRepository;
    @PostMapping("/book/{rideId}")
    public String bookRide(@PathVariable int rideId) {

        // Extract username from JWT (or use any auth logic)
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        customer user = customerRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check role
        if (!"USER".equalsIgnoreCase(user.getUserType())) {
            return "Only passengers can book rides";
        }

        Optional<Ride> optionalRide = rideRepository.findById(rideId);
        if (optionalRide.isEmpty()) {
            return "Ride not found";
        }

        Ride ride = optionalRide.get();

        if (ride.getAvailableSeats() <= 0) {
            return "No seats available";
        }

        // Reduce available seats
        ride.setAvailableSeats(ride.getAvailableSeats() - 1);
        rideRepository.save(ride);

        // Create Passenger entry
        Passenger passenger = new Passenger();
        passenger.setName(user.getUserName()); // assuming username as name
        passenger.setEmail(user.getEmail()); // example email
        passenger.setPhoneNumber(user.getPhoneNumber()); // default, can be updated later
        passenger.setRide(ride);
        passenger.setActive(true);
        passengerRepository.save(passenger);
        Booking booking = new Booking();
        booking.setPassengerId(passenger.getId()); // links to passenger
        booking.setPassengerName(passenger.getName());
        booking.setStatus("CONFIRMED");
        booking.setRide(ride);
        bookingRepository.save(booking);
        return "Booking confirmed for passenger: " + username + ", Passenger ID: " + passenger.getId();
    }
    @DeleteMapping("/cancel/{passengerId}")
    public String cancelBooking(@PathVariable int passengerId) {
        Optional<Passenger> optionalPassenger = passengerRepository.findById(passengerId);
        if (optionalPassenger.isEmpty()) {
            return "Passenger booking not found";
        }

        Passenger passenger = optionalPassenger.get();
        Ride ride = passenger.getRide();

        // Free up seat
        if (ride != null) {
            ride.setAvailableSeats(ride.getAvailableSeats() + 1);
            rideRepository.save(ride);
        }

        // Delete corresponding booking
        List<Booking> bookings = bookingRepository.findByPassengerId(passengerId);
        for (Booking b : bookings) {
            bookingRepository.delete(b);
        }

        // Delete passenger
        passengerRepository.delete(passenger);

        return "Booking cancelled and passenger removed from ride.";
    }


    // ✅ Get all passengers for a ride
//    @GetMapping("/ride/{rideId}/passengers")
//    public List<Booking> getPassengers(@PathVariable int rideId) {
//        return bookingRepository.findByRideId(rideId);
//    }
    // ✅ Get all passengers for rides posted by the logged-in driver
    @GetMapping("/driver/my")
    public List<Booking> getMyPassengers() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        customer driver = customerRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        if (!"DRIVER".equalsIgnoreCase(driver.getUserType())) {
            throw new RuntimeException("Only drivers can access this endpoint");
        }

        // Find all rides posted by this driver
        List<Ride> myRides = rideRepository.findByDriverName(username);

        // Collect all bookings for these rides
        List<Booking> allBookings = new java.util.ArrayList<>();
        for (Ride ride : myRides) {
            allBookings.addAll(bookingRepository.findByRideId(ride.getId()));
        }

        return allBookings;
    }

    @GetMapping("/my")
    public List<Booking> getMyBookings() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        customer user = customerRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Find bookings where passengerName (or passengerId) matches current user
        return bookingRepository.findByPassengerName(user.getUserName());
    }

}

package com.ashu.ride_sharing.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ashu.ride_sharing.dto.request.BookingRequest;
import com.ashu.ride_sharing.dto.response.BookingResponse;
import com.ashu.ride_sharing.dto.response.RideResponse;
import com.ashu.ride_sharing.dto.response.VehicleResponse;
import com.ashu.ride_sharing.models.Booking;
import com.ashu.ride_sharing.models.Notification;
import com.ashu.ride_sharing.models.Ride;
import com.ashu.ride_sharing.models.User;
import com.ashu.ride_sharing.models.Vehicle;
import com.ashu.ride_sharing.models.enums.BookingStatus;
import com.ashu.ride_sharing.models.enums.NotificationStatus;
import com.ashu.ride_sharing.models.enums.NotificationType;
import com.ashu.ride_sharing.models.enums.UserRole;
import com.ashu.ride_sharing.repositories.BookingRepository;
import com.ashu.ride_sharing.repositories.NotificationRepository;
import com.ashu.ride_sharing.repositories.RideRepository;
import com.ashu.ride_sharing.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public BookingResponse bookRide(String userEmail, BookingRequest request){
         User passenger = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Passenger not found"));
        
        Ride ride = rideRepository.findById(request.getRideId())
            .orElseThrow(()->new IllegalArgumentException("Ride not found"));

        validateBookingRequest(passenger, ride, request);

        boolean alreadyBooked = bookingRepository.existsByRide_RideIdAndPassenger_IdAndStatusNot(request.getRideId(), passenger.getId(), BookingStatus.CANCELLED);

        if (alreadyBooked) {
            throw new IllegalStateException("You already have an active booking for this ride");
        }

        BigDecimal totalFare = calculateFare(ride, request.getSeatsRequested());

        Booking booking = Booking.builder()
                .ride(ride)
                .passenger(passenger)
                .seatsBooked(request.getSeatsRequested())
                .totalFare(totalFare)
                .status(BookingStatus.PENDING)
                .pickupLocation(request.getPickupLocation())
                .dropoffLocation(request.getDropoffLocation())
                .build();
        
        ride.bookSeats(request.getSeatsRequested());
        
        Booking savedBooking = bookingRepository.save(booking);
        rideRepository.save(ride);

        log.info("Booking created: {} for ride {} by passenger {}", 
                savedBooking.getBookingId(), ride.getRideId(), userEmail);

        // Create notification for driver
        createBookingNotification(ride.getDriver(), passenger, ride);

        return mapToBookingResponse(savedBooking);

    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getPassengerBookings(String userEmail) {
        User passenger = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Passenger not found"));
        
        List<Booking> bookings = bookingRepository.findByPassenger_IdOrderByBookedAtDesc(passenger.getId());
        return bookings.stream()
        .map(this::mapToBookingResponse)
        .toList();
    }

    public List<BookingResponse> getDriverBookings(String userEmail) { // Changed method name
    User driver = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new UsernameNotFoundException("Driver not found"));

    List<Booking> bookings = bookingRepository.findByRide_Driver_IdOrderByBookedAtDesc(driver.getId());
    return bookings.stream()
        .map(this::mapToBookingResponse)
        .toList();
}
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(String userEmail, UUID bookingId){
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        boolean hasAccess = booking.getPassenger().getId().equals(user.getId()) ||
                           booking.getRide().getDriver().getId().equals(user.getId());
        
        if (!hasAccess) {
            throw new IllegalArgumentException("Access denied to this booking");
        }

        return mapToBookingResponse(booking);
    }


    public BookingResponse confirmBooking(String userEmail, UUID bookingId) {
        User driver = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Driver not found"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        // Validate driver owns this ride
        if (!booking.getRide().getDriver().getId().equals(driver.getId())) {
            throw new IllegalArgumentException("You can only confirm bookings for your rides");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Only pending bookings can be confirmed");
        }

        booking.confirm();
        Booking savedBooking = bookingRepository.save(booking);

        log.info("Booking confirmed by driver {}: {}", userEmail, bookingId);
        
        return mapToBookingResponse(savedBooking);
    }



    public BookingResponse cancelBooking(String userEmail, UUID bookingId, String reason) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        // Check if user can cancel this booking
        boolean canCancel = booking.getPassenger().getId().equals(user.getId()) ||
                           booking.getRide().getDriver().getId().equals(user.getId());
        
        if (!canCancel) {
            throw new IllegalArgumentException("You cannot cancel this booking");
        }

        if (!booking.canBeCancelled()) {
            throw new IllegalStateException("This booking cannot be cancelled");
        }

        // Restore seats to ride
        Ride ride = booking.getRide();
        ride.cancelSeats(booking.getSeatsBooked());
        
        booking.cancel(reason);
        
        bookingRepository.save(booking);
        rideRepository.save(ride);

        log.info("Booking cancelled by user {}: {} - Reason: {}", userEmail, bookingId, reason);
        
        // TODO: Process refund if payment was made
        // TODO: Send notification to other party
        
        return mapToBookingResponse(booking);
    }

        private void validateBookingRequest(User passenger, Ride ride, BookingRequest request) {
        // Check user role
        if (passenger.getRole() != UserRole.PASSENGER && passenger.getRole() != UserRole.DRIVER) {
            throw new IllegalStateException("Only passengers can book rides");
        }

        // Check if passenger is trying to book their own ride
        if (ride.getDriver().getId().equals(passenger.getId())) {
            throw new IllegalArgumentException("You cannot book your own ride");
        }

        // Check ride status and availability
        if (!ride.canBook(request.getSeatsRequested())) {
            throw new IllegalArgumentException("Ride cannot accommodate requested seats");
        }

        // Check if ride is not expired
        if (ride.isExpired()) {
            throw new IllegalArgumentException("Cannot book expired rides");
        }

        // Validate seat count
        if (request.getSeatsRequested() <= 0) {
            throw new IllegalArgumentException("Must request at least 1 seat");
        }

        // Check departure time (must be at least 1 hour from now)
        if (ride.getDepartureDateTime().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new IllegalArgumentException("Cannot book rides departing within 1 hour");
        }
    }
    

    private BigDecimal calculateFare(Ride ride, int seats){
        return ride.getBaseFare().multiply(new BigDecimal(seats));
    }




    private BookingResponse mapToBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .bookingReference(booking.getBookingReference())
                .ride(mapToRideResponse(booking.getRide()))
                .passengerName(booking.getPassenger().getFullName())
                .passengerEmail(booking.getPassenger().getEmail())
                .seatsBooked(booking.getSeatsBooked())
                .totalFare(booking.getTotalFare())
                .status(booking.getStatus())
                .pickupLocation(booking.getPickupLocation())
                .dropoffLocation(booking.getDropoffLocation())
                .bookedAt(booking.getBookedAt())
                .message("Booking processed successfully")
                .build();
    }

    private RideResponse mapToRideResponse(Ride ride) {
        return RideResponse.builder()
                .rideId(ride.getRideId())
                .driverName(ride.getDriver().getFullName())
                .vehicle(mapToVehicleResponse(ride.getVehicle()))
                .sourceCity(ride.getSourceCity())
                .sourceAddress(ride.getSourceAddress())
                .destinationCity(ride.getDestinationCity())
                .destinationAddress(ride.getDestinationAddress())
                .departureDateTime(ride.getDepartureDateTime())
                .availableSeats(ride.getAvailableSeats())
                .totalSeats(ride.getTotalSeats())
                .baseFare(ride.getBaseFare())
                .description(ride.getDescription())
                .status(ride.getStatus())
                .routeInfo(ride.getRouteInfo())
                .build();
    }

    private VehicleResponse mapToVehicleResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .vehicleId(vehicle.getVehicleId())
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .year(vehicle.getYear())
                .color(vehicle.getColor())
                .licensePlate(vehicle.getLicensePlate())
                .capacity(vehicle.getCapacity())
                .type(vehicle.getType())
                .status(vehicle.getStatus())
                .vehicleInfo(vehicle.getVehicleInfo())
                .images(vehicle.getVehicleImages())
                .build();
    }

    private void createBookingNotification(User driver, User passenger, Ride ride) {
        Notification notification = Notification.builder()
                .user(driver)
                .title("New Ride Booking Request")
                .message(String.format("%s has requested to book your ride from %s to %s", 
                        passenger.getFullName(), 
                        ride.getSourceCity(), 
                        ride.getDestinationCity()))
                .type(NotificationType.RIDE_REQUEST)
                .status(NotificationStatus.UNREAD)
                .relatedEntityType("BOOKING")
                .relatedEntityId(ride.getRideId().toString())
                .build();
        
        notificationRepository.save(notification);
        log.info("Notification created for driver {} about booking from passenger {}", 
                driver.getEmail(), passenger.getEmail());
    }
}

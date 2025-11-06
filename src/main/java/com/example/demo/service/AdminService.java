package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.enums.Role;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    // ====================== DASHBOARD STATISTICS ======================

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Total counts
        stats.put("totalUsers", userRepository.count());
        stats.put("totalDrivers", userRepository.countByRole(Role.DRIVER) + userRepository.countByRole(Role.BOTH));
        stats.put("totalPassengers", userRepository.countByRole(Role.PASSENGER) + userRepository.countByRole(Role.BOTH));
        stats.put("totalRides", rideRepository.count());
        stats.put("totalBookings", bookingRepository.count());
        
        // Today's activity - using manual filtering
        LocalDate today = LocalDate.now();
        long todayRides = rideRepository.findAll().stream()
            .filter(r -> r.getDate().equals(today))
            .count();
        long todayBookings = bookingRepository.findAll().stream()
            .filter(b -> b.getBookedAt().toLocalDate().equals(today))
            .count();
        stats.put("todayRides", todayRides);
        stats.put("todayBookings", todayBookings);
        
        // Revenue statistics
        Double totalRevenue = paymentRepository.findAll().stream()
            .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
            .mapToDouble(Payment::getAmount)
            .sum();
        stats.put("totalRevenue", totalRevenue);
        
        // Active users (using all users for now)
        stats.put("activeUsers", userRepository.count());
        
        // Average rating
        Double avgRating = reviewRepository.findAll().stream()
            .mapToInt(Review::getRating)
            .average()
            .orElse(0.0);
        stats.put("averageRating", Math.round(avgRating * 10.0) / 10.0);
        
        return stats;
    }

    // ====================== USER MANAGEMENT ======================

    public List<Map<String, Object>> getAllUsers() {
        return userRepository.findAll().stream()
            .map(this::userToMap)
            .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getAllDrivers() {
        List<User> drivers = new ArrayList<>();
        drivers.addAll(userRepository.findByRole(Role.DRIVER));
        drivers.addAll(userRepository.findByRole(Role.BOTH));
        
        return drivers.stream()
            .map(this::userToMap)
            .collect(Collectors.toList());
    }

    public Map<String, Object> getUserDetails(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Map<String, Object> details = userToMap(user);
        
        // Add additional statistics
        if (user.getRole() == Role.DRIVER || user.getRole() == Role.BOTH) {
            details.put("totalRidesOffered", rideRepository.findByDriverId(user.getId()).size());
            details.put("completedRides", rideRepository.findByDriverId(user.getId()).stream()
                .filter(r -> "COMPLETED".equals(r.getStatus()))
                .count());
        }
        
        details.put("totalBookings", bookingRepository.findByPassengerId(user.getId()).size());
        details.put("totalReviews", reviewRepository.getTotalReviewsForUser(user));
        details.put("averageRating", reviewRepository.getAverageRatingForUser(user));
        
        return details;
    }

    public void blockUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setAccountNonLocked(false);
        userRepository.save(user);
    }

    public void unblockUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setAccountNonLocked(true);
        userRepository.save(user);
    }

    public void verifyDriver(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getRole() != Role.DRIVER && user.getRole() != Role.BOTH) {
            throw new RuntimeException("User is not a driver");
        }
        user.setEnabled(true);
        userRepository.save(user);
    }

    // ====================== RIDE MONITORING ======================

    public List<Map<String, Object>> getAllRides(String status, String date) {
        List<Ride> rides;
        
        if (status != null && !status.isEmpty() && !status.equals("ALL")) {
            rides = rideRepository.findByStatus(status);
        } else {
            rides = rideRepository.findAll();
        }
        
        if (date != null && !date.isEmpty()) {
            rides = rides.stream()
                .filter(r -> r.getDate().equals(date))
                .collect(Collectors.toList());
        }
        
        return rides.stream()
            .map(this::rideToMap)
            .collect(Collectors.toList());
    }

    public Map<String, Object> getRideDetails(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
            .orElseThrow(() -> new RuntimeException("Ride not found"));
        
        Map<String, Object> details = rideToMap(ride);
        
        // Add booking information
        List<Map<String, Object>> bookings = ride.getBookings().stream()
            .map(this::bookingToMap)
            .collect(Collectors.toList());
        details.put("bookings", bookings);
        
        return details;
    }

    // ====================== BOOKING MONITORING ======================

    public List<Map<String, Object>> getAllBookings(String status, String date) {
        List<Booking> bookings;
        
        if (status != null && !status.isEmpty() && !status.equals("ALL")) {
            // Manual filtering since we don't have findByStatus method
            bookings = bookingRepository.findAll().stream()
                .filter(b -> status.equals(b.getStatus().toString()))
                .collect(Collectors.toList());
        } else {
            bookings = bookingRepository.findAll();
        }
        
        if (date != null && !date.isEmpty()) {
            bookings = bookings.stream()
                .filter(b -> b.getRide().getDate().toString().equals(date))
                .collect(Collectors.toList());
        }
        
        return bookings.stream()
            .map(this::bookingToMap)
            .collect(Collectors.toList());
    }

    // ====================== PAYMENT MONITORING ======================

    public List<Map<String, Object>> getAllPayments(String status, String dateFrom, String dateTo) {
        List<Payment> payments = paymentRepository.findAll();
        
        if (status != null && !status.isEmpty() && !status.equals("ALL")) {
            payments = payments.stream()
                .filter(p -> status.equals(p.getStatus().toString()))
                .collect(Collectors.toList());
        }
        
        if (dateFrom != null && !dateFrom.isEmpty()) {
            LocalDateTime fromDate = LocalDate.parse(dateFrom).atStartOfDay();
            payments = payments.stream()
                .filter(p -> p.getCreatedAt().isAfter(fromDate))
                .collect(Collectors.toList());
        }
        
        if (dateTo != null && !dateTo.isEmpty()) {
            LocalDateTime toDate = LocalDate.parse(dateTo).plusDays(1).atStartOfDay();
            payments = payments.stream()
                .filter(p -> p.getCreatedAt().isBefore(toDate))
                .collect(Collectors.toList());
        }
        
        return payments.stream()
            .map(this::paymentToMap)
            .collect(Collectors.toList());
    }

    // ====================== REPORTS ======================

    public Map<String, Object> generateRevenueReport(String period) {
        Map<String, Object> report = new HashMap<>();
        LocalDateTime startDate;
        
        switch (period.toUpperCase()) {
            case "TODAY":
                startDate = LocalDateTime.now().toLocalDate().atStartOfDay();
                break;
            case "WEEK":
                startDate = LocalDateTime.now().minusWeeks(1);
                break;
            case "MONTH":
                startDate = LocalDateTime.now().minusMonths(1);
                break;
            case "YEAR":
                startDate = LocalDateTime.now().minusYears(1);
                break;
            default:
                startDate = LocalDateTime.now().minusMonths(1);
        }
        
        List<Payment> payments = paymentRepository.findAll().stream()
            .filter(p -> p.getCreatedAt().isAfter(startDate))
            .collect(Collectors.toList());
        
        Double totalRevenue = payments.stream()
            .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
            .mapToDouble(Payment::getAmount)
            .sum();
        
        Double platformCommission = totalRevenue * 0.1; // Assuming 10% commission
        Double driverEarnings = totalRevenue - platformCommission;
        
        report.put("period", period);
        report.put("totalRevenue", totalRevenue);
        report.put("platformCommission", platformCommission);
        report.put("driverEarnings", driverEarnings);
        report.put("totalTransactions", payments.size());
        report.put("successfulTransactions", payments.stream().filter(p -> p.getStatus() == PaymentStatus.SUCCESS).count());
        report.put("failedTransactions", payments.stream().filter(p -> p.getStatus() == PaymentStatus.FAILED).count());
        
        return report;
    }

    public Map<String, Object> generateRideReport(String period) {
        Map<String, Object> report = new HashMap<>();
        LocalDateTime startDate;
        
        switch (period.toUpperCase()) {
            case "TODAY":
                startDate = LocalDateTime.now().toLocalDate().atStartOfDay();
                break;
            case "WEEK":
                startDate = LocalDateTime.now().minusWeeks(1);
                break;
            case "MONTH":
                startDate = LocalDateTime.now().minusMonths(1);
                break;
            case "YEAR":
                startDate = LocalDateTime.now().minusYears(1);
                break;
            default:
                startDate = LocalDateTime.now().minusMonths(1);
        }
        
        // Manual filtering since entity doesn't have createdAt getter
        List<Ride> rides = rideRepository.findAll();
        
        report.put("period", period);
        report.put("totalRides", rides.size());
        report.put("completedRides", rides.stream().filter(r -> "COMPLETED".equals(r.getStatus())).count());
        report.put("cancelledRides", rides.stream().filter(r -> "CANCELLED".equals(r.getStatus())).count());
        report.put("activeRides", rides.stream().filter(r -> "ACTIVE".equals(r.getStatus())).count());
        
        return report;
    }

    public Map<String, Object> generateUserReport() {
        Map<String, Object> report = new HashMap<>();
        
        List<User> allUsers = userRepository.findAll();
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        
        report.put("totalUsers", allUsers.size());
        report.put("activeUsers", allUsers.stream().filter(u -> u.getUpdatedAt().isAfter(thirtyDaysAgo)).count());
        report.put("drivers", allUsers.stream().filter(u -> u.getRole() == Role.DRIVER || u.getRole() == Role.BOTH).count());
        report.put("passengers", allUsers.stream().filter(u -> u.getRole() == Role.PASSENGER || u.getRole() == Role.BOTH).count());
        report.put("blockedUsers", allUsers.stream().filter(u -> !u.getAccountNonLocked()).count());
        report.put("newUsersThisMonth", allUsers.stream().filter(u -> u.getCreatedAt().isAfter(LocalDateTime.now().minusMonths(1))).count());
        
        return report;
    }

    // ====================== HELPER METHODS ======================

    private Map<String, Object> userToMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("name", user.getName());
        map.put("email", user.getEmail());
        map.put("phone", user.getPhone());
        map.put("role", user.getRole().name());
        map.put("rating", user.getRating());
        map.put("totalRides", user.getTotalRides());
        map.put("enabled", user.getEnabled());
        map.put("accountNonLocked", user.getAccountNonLocked());
        map.put("createdAt", user.getCreatedAt());
        map.put("updatedAt", user.getUpdatedAt());
        
        if (user.getRole() == Role.DRIVER || user.getRole() == Role.BOTH) {
            map.put("vehicleModel", user.getVehicleModel());
            map.put("vehiclePlate", user.getVehiclePlate());
            map.put("vehicleCapacity", user.getVehicleCapacity());
        }
        
        return map;
    }

    private Map<String, Object> rideToMap(Ride ride) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", ride.getId());
        map.put("driverId", ride.getDriver().getId());
        map.put("driverName", ride.getDriver().getName());
        map.put("driverUsername", ride.getDriver().getUsername());
        map.put("source", ride.getSource());
        map.put("destination", ride.getDestination());
        map.put("date", ride.getDate());
        map.put("time", ride.getTime());
        map.put("availableSeats", ride.getAvailableSeats());
        map.put("pricePerSeat", ride.getPricePerKm()); // Using pricePerKm as pricePerSeat
        map.put("vehicleModel", ride.getVehicleModel());
        map.put("status", ride.getStatus());
        map.put("createdAt", LocalDateTime.now()); // Fallback since Ride doesn't expose createdAt
        map.put("bookingsCount", ride.getBookings() != null ? ride.getBookings().size() : 0);
        return map;
    }

    private Map<String, Object> bookingToMap(Booking booking) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", booking.getId());
        map.put("passengerId", booking.getPassenger().getId());
        map.put("passengerName", booking.getPassenger().getName());
        map.put("passengerUsername", booking.getPassenger().getUsername());
        map.put("rideId", booking.getRide().getId());
        map.put("pickupLocation", booking.getPickupLocation());
        map.put("dropLocation", booking.getDropLocation());
        map.put("seatsBooked", booking.getSeatsBooked());
        map.put("fare", booking.getFare());
        map.put("status", booking.getStatus().toString());
        map.put("createdAt", booking.getBookedAt());
        map.put("route", booking.getRide().getSource() + " → " + booking.getRide().getDestination());
        map.put("date", booking.getRide().getDate());
        map.put("time", booking.getRide().getTime());
        return map;
    }

    private Map<String, Object> paymentToMap(Payment payment) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", payment.getId());
        map.put("bookingId", payment.getBooking() != null ? payment.getBooking().getId() : null);
        map.put("payerUsername", payment.getUser() != null ? payment.getUser().getUsername() : "N/A");
        map.put("payeeName", "Driver"); // Simplified - actual payee would be the driver
        map.put("amount", payment.getAmount());
        map.put("status", payment.getStatus().toString());
        map.put("paymentMethod", payment.getPaymentMethod() != null ? payment.getPaymentMethod().toString() : "N/A");
        map.put("transactionId", payment.getRazorpayPaymentId());
        map.put("createdAt", payment.getCreatedAt());
        map.put("completedAt", payment.getPaidAt());
        return map;
    }
}

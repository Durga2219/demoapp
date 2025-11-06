package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.entity.Ride;
import com.example.demo.entity.User;
import com.example.demo.enums.Role;
import com.example.demo.repository.RideRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/data")
@CrossOrigin(origins = "*")
public class DataController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private WalletService walletService;

    @PostMapping("/setup")
    public ResponseEntity<ApiResponse> setupTestData() {
        try {
            // Create test users if they don't exist
            createTestUsers();
            
            // Create test rides
            createTestRides();
            
            return ResponseEntity.ok(new ApiResponse(true, "Test data created successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Failed to create test data: " + e.getMessage(), null));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse> getDataStatus() {
        try {
            long userCount = userRepository.count();
            long rideCount = rideRepository.count();
            long activeRides = rideRepository.countByStatus("ACTIVE");
            
            String status = String.format("Users: %d, Rides: %d, Active: %d", userCount, rideCount, activeRides);
            return ResponseEntity.ok(new ApiResponse(true, "Database status", status));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Failed to get status: " + e.getMessage(), null));
        }
    }

    @GetMapping("/rides")
    public ResponseEntity<ApiResponse> getPublicRides() {
        try {
            List<Ride> rides = rideRepository.findAllActiveRides();
            return ResponseEntity.ok(new ApiResponse(true, "Active rides retrieved", rides));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Failed to get rides: " + e.getMessage(), null));
        }
    }

    private void createTestUsers() {
        // Create test driver
        if (!userRepository.existsByEmail("driver@test.com")) {
            User driver = new User();
            driver.setUsername("Test Driver");
            driver.setName("Test Driver"); // Set the name field
            driver.setEmail("driver@test.com");
            driver.setPhone("9876543210");
            driver.setPassword(passwordEncoder.encode("password123"));
            driver.setRole(Role.DRIVER);
            driver.setVehicleModel("Toyota Camry");
            driver.setVehiclePlate("ABC-1234");
            userRepository.save(driver);
            
            // Create wallet for driver
            walletService.createWallet(driver);
        }

        // Create test passenger
        if (!userRepository.existsByEmail("passenger@test.com")) {
            User passenger = new User();
            passenger.setUsername("Test Passenger");
            passenger.setName("Test Passenger"); // Set the name field
            passenger.setEmail("passenger@test.com");
            passenger.setPhone("9876543211");
            passenger.setPassword(passwordEncoder.encode("password123"));
            passenger.setRole(Role.PASSENGER);
            userRepository.save(passenger);
            
            // Create wallet for passenger
            walletService.createWallet(passenger);
        }

        // Create admin user
        if (!userRepository.existsByEmail("admin@rideconnect.com")) {
            User admin = new User();
            admin.setUsername("Admin");
            admin.setName("System Administrator");
            admin.setEmail("admin@rideconnect.com");
            admin.setPhone("9999999999");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            
            // Create wallet for admin
            walletService.createWallet(admin);
        }

        // Create simple admin user as backup
        if (!userRepository.existsByEmail("admin@test.com")) {
            User admin = new User();
            admin.setUsername("Admin User");
            admin.setName("Administrator");
            admin.setEmail("admin@test.com");
            admin.setPhone("1111111111");
            admin.setPassword(passwordEncoder.encode("password123"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            
            // Create wallet for admin
            walletService.createWallet(admin);
        }
    }

    private void createTestRides() {
        User driver = userRepository.findByEmail("driver@test.com").orElse(null);
        if (driver == null) return;

        // Clear existing rides for clean test
        List<Ride> existingRides = rideRepository.findByDriverId(driver.getId());
        if (existingRides.size() < 3) {
            // Create test rides for today and tomorrow
            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);

            // Ride 1: Today
            Ride ride1 = new Ride();
            ride1.setDriver(driver);
            ride1.setSource("Mumbai Central");
            ride1.setDestination("Pune Station");
            ride1.setDate(today);
            ride1.setTime(LocalTime.of(14, 30));
            ride1.setTotalSeats(4);
            ride1.setAvailableSeats(4);
            ride1.setFare(500.0);
            ride1.setPricePerKm(8.0);
            ride1.setStatus("ACTIVE");
            ride1.setVehicleModel(driver.getVehicleModel());
            ride1.setVehiclePlate(driver.getVehiclePlate());
            rideRepository.save(ride1);

            // Ride 2: Tomorrow
            Ride ride2 = new Ride();
            ride2.setDriver(driver);
            ride2.setSource("Delhi");
            ride2.setDestination("Agra");
            ride2.setDate(tomorrow);
            ride2.setTime(LocalTime.of(9, 0));
            ride2.setTotalSeats(3);
            ride2.setAvailableSeats(3);
            ride2.setFare(800.0);
            ride2.setPricePerKm(10.0);
            ride2.setStatus("ACTIVE");
            ride2.setVehicleModel(driver.getVehicleModel());
            ride2.setVehiclePlate(driver.getVehiclePlate());
            rideRepository.save(ride2);

            // Ride 3: Tomorrow evening
            Ride ride3 = new Ride();
            ride3.setDriver(driver);
            ride3.setSource("Bangalore");
            ride3.setDestination("Mysore");
            ride3.setDate(tomorrow);
            ride3.setTime(LocalTime.of(18, 0));
            ride3.setTotalSeats(2);
            ride3.setAvailableSeats(2);
            ride3.setFare(300.0);
            ride3.setPricePerKm(6.0);
            ride3.setStatus("ACTIVE");
            ride3.setVehicleModel(driver.getVehicleModel());
            ride3.setVehiclePlate(driver.getVehiclePlate());
            rideRepository.save(ride3);
        }
    }
}
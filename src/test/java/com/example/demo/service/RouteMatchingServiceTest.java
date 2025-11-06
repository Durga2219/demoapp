package com.example.demo.service;

import com.example.demo.entity.Ride;
import com.example.demo.entity.User;
import com.example.demo.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteMatchingServiceTest {

    @Mock
    private GoogleMapsService googleMapsService;

    @InjectMocks
    private RouteMatchingService routeMatchingService;

    private List<Ride> testRides;
    private User testDriver;

    @BeforeEach
    void setUp() {
        // Create test driver
        testDriver = new User();
        testDriver.setId(1L);
        testDriver.setUsername("Test Driver");
        testDriver.setEmail("driver@test.com");
        testDriver.setRole(Role.DRIVER);
        testDriver.setVehicleModel("Toyota Camry");
        testDriver.setVehiclePlate("ABC-1234");

        // Create test rides
        testRides = Arrays.asList(
            createTestRide(null, "Mumbai Central", "Pune Station", LocalDate.now(), 4),
            createTestRide(null, "Delhi", "Agra", LocalDate.now().plusDays(1), 3),
            createTestRide(null, "Bangalore", "Mysore", LocalDate.now().plusDays(1), 2),
            createTestRide(null, "Chennai", "Bangalore", LocalDate.now(), 4)
        );
    }

    private Ride createTestRide(Long id, String source, String destination, LocalDate date, int seats) {
        Ride ride = new Ride();
        // Note: ID is auto-generated, so we don't set it manually
        ride.setDriver(testDriver);
        ride.setSource(source);
        ride.setDestination(destination);
        ride.setDate(date);
        ride.setTime(LocalTime.of(10, 0));
        ride.setTotalSeats(seats);
        ride.setAvailableSeats(seats);
        ride.setFare(500.0);
        ride.setPricePerKm(8.0);
        ride.setStatus("ACTIVE");
        return ride;
    }

    @Test
    void testFindDirectMatches_ExactMatch() {
        // Test exact route match
        List<RouteMatchingService.RouteMatch> matches = 
            routeMatchingService.findDirectMatches("Mumbai Central", "Pune Station", testRides);

        assertEquals(1, matches.size());
        RouteMatchingService.RouteMatch match = matches.get(0);
        assertEquals(100.0, match.getMatchQuality());
        assertEquals("DIRECT", match.getMatchType());
        assertEquals(0.0, match.getRouteDeviation());
        assertEquals("Mumbai Central", match.getRide().getSource());
        assertEquals("Pune Station", match.getRide().getDestination());
    }

    @Test
    void testFindDirectMatches_NoMatch() {
        // Test no direct match
        List<RouteMatchingService.RouteMatch> matches = 
            routeMatchingService.findDirectMatches("Kolkata", "Bhubaneswar", testRides);

        assertEquals(0, matches.size());
    }

    @Test
    void testFindPartialMatches_WithMockedGoogleMaps() throws Exception {
        // Mock Google Maps API responses
        when(googleMapsService.calculateDistance("Mumbai Central", "Pune Station")).thenReturn(150.0);
        when(googleMapsService.calculateDistance("Mumbai", "Pune")).thenReturn(145.0);
        when(googleMapsService.calculateDistance("Mumbai", "Mumbai Central")).thenReturn(5.0);
        when(googleMapsService.calculateDistance("Mumbai", "Pune Station")).thenReturn(148.0);
        when(googleMapsService.calculateDistance("Pune", "Mumbai Central")).thenReturn(152.0);
        when(googleMapsService.calculateDistance("Pune", "Pune Station")).thenReturn(3.0);

        // Test partial match
        List<RouteMatchingService.RouteMatch> matches = 
            routeMatchingService.findPartialMatches("Mumbai", "Pune", testRides);

        // Should find at least one partial match
        assertTrue(matches.size() > 0);
        
        // Check that matches are sorted by quality
        for (int i = 0; i < matches.size() - 1; i++) {
            assertTrue(matches.get(i).getMatchQuality() >= matches.get(i + 1).getMatchQuality());
        }
    }

    @Test
    void testFindAllMatches_CombinesDirectAndPartial() throws Exception {
        // Mock Google Maps for partial matching
        when(googleMapsService.calculateDistance(anyString(), anyString())).thenReturn(100.0);

        // Test finding all matches
        List<RouteMatchingService.RouteMatch> allMatches = 
            routeMatchingService.findAllMatches("Mumbai Central", "Pune Station", testRides);

        // Should find the direct match first
        assertTrue(allMatches.size() >= 1);
        RouteMatchingService.RouteMatch firstMatch = allMatches.get(0);
        assertEquals(100.0, firstMatch.getMatchQuality()); // Direct match should be first
        assertEquals("DIRECT", firstMatch.getMatchType());
    }

    @Test
    void testFindAllMatches_SortedByQuality() throws Exception {
        // Mock Google Maps responses for different quality matches
        when(googleMapsService.calculateDistance("Chennai", "Bangalore")).thenReturn(350.0);
        when(googleMapsService.calculateDistance("Chennai", "Mysore")).thenReturn(480.0);
        when(googleMapsService.calculateDistance("Mysore", "Chennai")).thenReturn(480.0);
        when(googleMapsService.calculateDistance("Mysore", "Bangalore")).thenReturn(140.0);

        List<RouteMatchingService.RouteMatch> matches = 
            routeMatchingService.findAllMatches("Chennai", "Mysore", testRides);

        // Verify matches are sorted by quality (highest first)
        for (int i = 0; i < matches.size() - 1; i++) {
            assertTrue(matches.get(i).getMatchQuality() >= matches.get(i + 1).getMatchQuality(),
                "Matches should be sorted by quality in descending order");
        }
    }

    @Test
    void testRouteMatchQualityThreshold() throws Exception {
        // Mock very poor matches (should be filtered out)
        when(googleMapsService.calculateDistance(anyString(), anyString())).thenReturn(1000.0);

        List<RouteMatchingService.RouteMatch> matches = 
            routeMatchingService.findPartialMatches("Kolkata", "Bhubaneswar", testRides);

        // Should filter out very poor matches (below 60% threshold)
        for (RouteMatchingService.RouteMatch match : matches) {
            assertTrue(match.getMatchQuality() >= 60.0, 
                "All matches should meet minimum quality threshold of 60%");
        }
    }

    @Test
    void testMatchTypeClassification() {
        // Test direct match classification
        List<RouteMatchingService.RouteMatch> directMatches = 
            routeMatchingService.findDirectMatches("Delhi", "Agra", testRides);

        assertEquals(1, directMatches.size());
        assertEquals("DIRECT", directMatches.get(0).getMatchType());
        assertEquals(100.0, directMatches.get(0).getMatchQuality());
    }
}
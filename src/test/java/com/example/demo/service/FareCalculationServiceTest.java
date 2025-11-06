package com.example.demo.service;

import com.example.demo.dto.FareRequest;
import com.example.demo.dto.FareResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

public class FareCalculationServiceTest {

    private FareCalculationService fareCalculationService;
    private GoogleMapsService googleMapsService;

    @BeforeEach
    void setUp() {
        googleMapsService = new GoogleMapsService();
        fareCalculationService = new FareCalculationService();
        
        // Inject the GoogleMapsService using reflection
        ReflectionTestUtils.setField(fareCalculationService, "googleMapsService", googleMapsService);
        
        // Set up test API configuration
        ReflectionTestUtils.setField(googleMapsService, "apiKey", "test_key");
        ReflectionTestUtils.setField(googleMapsService, "apiUrl", "https://test.api.url");
    }

    @Test
    void testCalculateFareWithValidInput() {
        // Arrange
        FareRequest request = new FareRequest("Chennai", "Bangalore", 1);

        // Act
        FareResponse response = fareCalculationService.calculateFare(request);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getFareDetails());
        assertEquals(350.0, response.getFareDetails().getDistance()); // Fallback distance for Chennai-Bangalore
        assertEquals(50.0, response.getFareDetails().getBaseFare());
        assertEquals(12.0, response.getFareDetails().getPricePerKm());
        assertEquals(4200.0, response.getFareDetails().getDistanceFare()); // 350 * 12
        assertEquals(4250.0, response.getFareDetails().getTotalFare()); // 50 + 4200
        assertEquals(4250.0, response.getFareDetails().getFarePerPassenger()); // 4250 / 1
    }

    @Test
    void testCalculateFareWithMultiplePassengers() {
        // Arrange
        FareRequest request = new FareRequest("Mumbai", "Pune", 3);

        // Act
        FareResponse response = fareCalculationService.calculateFare(request);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(150.0, response.getFareDetails().getDistance()); // Fallback distance for Mumbai-Pune
        assertEquals(1800.0, response.getFareDetails().getDistanceFare()); // 150 * 12
        assertEquals(1850.0, response.getFareDetails().getTotalFare()); // 50 + 1800
        assertEquals(616.67, response.getFareDetails().getFarePerPassenger(), 0.01); // 1850 / 3
    }

    @Test
    void testCalculateFareWithFallbackDistance() {
        // Arrange
        FareRequest request = new FareRequest("Unknown City", "Another City", 1);

        // Act
        FareResponse response = fareCalculationService.calculateFare(request);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(100.0, response.getFareDetails().getDistance()); // Default fallback distance
        assertEquals(1200.0, response.getFareDetails().getDistanceFare()); // 100 * 12
        assertEquals(1250.0, response.getFareDetails().getTotalFare()); // 50 + 1200
        assertEquals("GOOGLE_MAPS", response.getFareDetails().getCalculationMethod()); // System attempts Google Maps but falls back
    }

    @Test
    void testCalculateFareWithInvalidInput() {
        // Arrange
        FareRequest request = new FareRequest("", "Bangalore", 1);

        // Act
        FareResponse response = fareCalculationService.calculateFare(request);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("Source location is required", response.getMessage());
    }

    @Test
    void testCalculateProportionalFare() {
        // Act
        FareResponse response = fareCalculationService.calculateProportionalFare(1L, 2, 4, 1000.0);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(500.0, response.getFareDetails().getTotalFare()); // (1000 / 4) * 2
        assertEquals(250.0, response.getFareDetails().getFarePerPassenger()); // 1000 / 4
    }

    @Test
    void testCalculateDriverEarnings() {
        // Act
        double earnings = fareCalculationService.calculateDriverEarnings(1000.0);

        // Assert
        assertEquals(900.0, earnings); // 1000 - (1000 * 0.10)
    }

    @Test
    void testCalculatePlatformCommission() {
        // Act
        double commission = fareCalculationService.calculatePlatformCommission(1000.0);

        // Assert
        assertEquals(100.0, commission); // 1000 * 0.10
    }
}
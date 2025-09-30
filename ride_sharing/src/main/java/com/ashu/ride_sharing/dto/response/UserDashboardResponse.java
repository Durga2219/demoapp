package com.ashu.ride_sharing.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDashboardResponse {
    private UserProfileResponse profile;
    private DashboardStats stats;
    private List<RideResponse> recentRides;
    private List<BookingResponse> recentBookings;
    private List<VehicleResponse> vehicles; // For drivers only
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DashboardStats {
        private Long totalRides;
        private Long totalBookings;
        private Long completedRides;
        private Long cancelledBookings;
        private java.math.BigDecimal totalEarnings; // For drivers
        private java.math.BigDecimal totalSpent; // For passengers
    }
}

package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.ReviewRequest;
import com.example.demo.entity.Review;
import com.example.demo.entity.ReviewType;
import com.example.demo.entity.User;
import com.example.demo.service.ReviewService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserService userService;

    // Create a new review (supports both passenger-to-driver and driver-to-passenger)
    @PostMapping
    public ResponseEntity<ApiResponse> createReview(@RequestBody Map<String, Object> reviewData,
                                                   Authentication authentication) {
        try {
            // Extract data from request
            Long bookingId = reviewData.get("bookingId") != null ? Long.valueOf(reviewData.get("bookingId").toString()) : null;
            Long driverId = reviewData.get("driverId") != null ? Long.valueOf(reviewData.get("driverId").toString()) : null;
            Long passengerId = reviewData.get("passengerId") != null ? Long.valueOf(reviewData.get("passengerId").toString()) : null;
            Integer rating = reviewData.get("rating") != null ? Integer.valueOf(reviewData.get("rating").toString()) : null;
            String comment = reviewData.containsKey("comment") ? reviewData.get("comment").toString() : "";
            String reviewTypeStr = reviewData.containsKey("reviewType") ? reviewData.get("reviewType").toString() : "PASSENGER_TO_DRIVER";
            
            // Validate required fields
            if (bookingId == null || rating == null) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Missing required fields: bookingId or rating", null));
            }
            
            // Determine review type
            ReviewType reviewType = ReviewType.valueOf(reviewTypeStr);
            
            // Validate based on review type
            if (reviewType == ReviewType.PASSENGER_TO_DRIVER && driverId == null) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Missing driverId for passenger-to-driver review", null));
            }
            if (reviewType == ReviewType.DRIVER_TO_PASSENGER && passengerId == null) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Missing passengerId for driver-to-passenger review", null));
            }
            
            // Handle photos if provided
            String photosJson = null;
            if (reviewData.containsKey("photos")) {
                @SuppressWarnings("unchecked")
                List<String> photos = (List<String>) reviewData.get("photos");
                if (photos != null && !photos.isEmpty()) {
                    // Convert photos list to JSON string
                    photosJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(photos);
                }
            }
            
            User reviewer = userService.findByUsername(authentication.getName());
            
            // Create review using bookingId
            Review review = reviewService.createReview(
                bookingId,
                reviewer,
                rating,
                comment,
                reviewType
            );

            return ResponseEntity.ok(new ApiResponse(true, "✅ Review submitted successfully! Thank you for your feedback.", review.getId()));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Invalid rating, booking ID, or driver ID", null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Failed to submit review: " + e.getMessage(), null));
        }
    }

    // Original create review method (with bookingId)
    @PostMapping("/booking")
    public ResponseEntity<ApiResponse> createReviewByBooking(@Valid @RequestBody ReviewRequest request, 
                                                   BindingResult result, 
                                                   Authentication authentication) {
        if (result.hasErrors()) {
            String errorMsg = result.getFieldError() != null 
                ? result.getFieldError().getDefaultMessage() 
                : "Invalid review data";
            return ResponseEntity.badRequest().body(new ApiResponse(false, errorMsg, null));
        }

        try {
            User reviewer = userService.findByUsername(authentication.getName());
            ReviewType reviewType = ReviewType.valueOf(request.getReviewType());

            Review review = reviewService.createReview(
                request.getBookingId(),
                reviewer,
                request.getRating(),
                request.getComment(),
                reviewType
            );

            return ResponseEntity.ok(new ApiResponse(true, "Review submitted successfully", review.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Failed to submit review: " + e.getMessage(), null));
        }
    }

    // Get reviews for a user
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse> getUserReviews(@PathVariable Long userId) {
        try {
            User user = userService.findById(userId);
            List<Review> reviews = reviewService.getReviewsForUser(user);
            
            return ResponseEntity.ok(new ApiResponse(true, "Reviews retrieved successfully", reviews));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Failed to retrieve reviews: " + e.getMessage(), null));
        }
    }

    // Get user rating summary
    @GetMapping("/summary/{userId}")
    public ResponseEntity<ApiResponse> getUserRatingSummary(@PathVariable Long userId) {
        try {
            User user = userService.findById(userId);
            ReviewService.UserRatingSummary summary = reviewService.getUserRatingSummary(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("averageRating", summary.getAverageRating());
            response.put("totalReviews", summary.getTotalReviews());
            response.put("recentReviews", summary.getRecentReviews());
            
            return ResponseEntity.ok(new ApiResponse(true, "Rating summary retrieved successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Failed to retrieve rating summary: " + e.getMessage(), null));
        }
    }

    // Check if user can review a booking
    @GetMapping("/can-review/{bookingId}")
    public ResponseEntity<ApiResponse> canReview(@PathVariable Long bookingId, 
                                               @RequestParam String reviewType,
                                               Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            ReviewType type = ReviewType.valueOf(reviewType);
            
            boolean canReview = reviewService.canReview(bookingId, user, type);
            
            Map<String, Object> response = new HashMap<>();
            response.put("canReview", canReview);
            response.put("bookingId", bookingId);
            response.put("reviewType", reviewType);
            
            return ResponseEntity.ok(new ApiResponse(true, "Review eligibility checked", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Failed to check review eligibility: " + e.getMessage(), null));
        }
    }

    // Get my reviews (reviews I've given)
    @GetMapping("/my-reviews")
    public ResponseEntity<ApiResponse> getMyReviews(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            List<Review> reviews = reviewService.getReviewsByUser(user);
            
            return ResponseEntity.ok(new ApiResponse(true, "My reviews retrieved successfully", reviews));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Failed to retrieve my reviews: " + e.getMessage(), null));
        }
    }

    // Get my rating summary
    @GetMapping("/my-summary")
    public ResponseEntity<ApiResponse> getMyRatingSummary(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            ReviewService.UserRatingSummary summary = reviewService.getUserRatingSummary(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("averageRating", summary.getAverageRating());
            response.put("totalReviews", summary.getTotalReviews());
            response.put("recentReviews", summary.getRecentReviews());
            
            return ResponseEntity.ok(new ApiResponse(true, "My rating summary retrieved successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Failed to retrieve my rating summary: " + e.getMessage(), null));
        }
    }
}
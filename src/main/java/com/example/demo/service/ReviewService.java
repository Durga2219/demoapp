package com.example.demo.service;

import com.example.demo.entity.Review;
import com.example.demo.entity.ReviewType;
import com.example.demo.entity.User;
import com.example.demo.entity.Booking;
import com.example.demo.entity.Ride;
import com.example.demo.repository.ReviewRepository;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.RideRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private NotificationService notificationService;

    // Create a new review
    public Review createReview(Long bookingId, User reviewer, Integer rating, String comment, ReviewType reviewType) {
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            throw new RuntimeException("Booking not found");
        }

        Booking booking = bookingOpt.get();
        
        // Check if review already exists
        if (reviewRepository.existsByBookingAndReviewType(booking, reviewType)) {
            throw new RuntimeException("Review already exists for this booking");
        }

        // Determine reviewee based on review type
        User reviewee;
        if (reviewType == ReviewType.PASSENGER_TO_DRIVER) {
            reviewee = booking.getRide().getDriver();
            // Verify reviewer is the passenger
            if (!booking.getPassenger().getId().equals(reviewer.getId())) {
                throw new RuntimeException("Only the passenger can review the driver");
            }
        } else {
            reviewee = booking.getPassenger();
            // Verify reviewer is the driver
            if (!booking.getRide().getDriver().getId().equals(reviewer.getId())) {
                throw new RuntimeException("Only the driver can review the passenger");
            }
        }

        // Validate rating
        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        Review review = new Review(booking, reviewer, reviewee, rating, comment, reviewType);
        Review savedReview = reviewRepository.save(review);

        // Send notification to reviewee
        notificationService.sendReviewReceivedNotification(savedReview);

        return savedReview;
    }

    // Create review for a ride (simplified - finds passenger's booking automatically)
    public Review createReviewForRide(Long rideId, User passenger, Integer rating, String comment, String photosJson) {
        // Find the ride
        Optional<Ride> rideOpt = rideRepository.findById(rideId);
        if (rideOpt.isEmpty()) {
            throw new RuntimeException("Ride not found");
        }
        Ride ride = rideOpt.get();

        // Find passenger's booking for this ride
        List<Booking> bookings = bookingRepository.findByRideAndPassenger(ride, passenger);
        if (bookings.isEmpty()) {
            throw new RuntimeException("You haven't booked this ride");
        }
        Booking booking = bookings.get(0); // Get first booking

        // Check if review already exists
        if (reviewRepository.existsByBookingAndReviewType(booking, ReviewType.PASSENGER_TO_DRIVER)) {
            throw new RuntimeException("You have already reviewed this ride");
        }

        // Validate rating
        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        // Create review
        Review review = new Review();
        review.setBooking(booking);
        review.setReviewer(passenger);
        review.setReviewee(ride.getDriver());
        review.setRating(rating);
        review.setComment(comment);
        review.setReviewType(ReviewType.PASSENGER_TO_DRIVER);
        review.setPhotos(photosJson); // Store photos as JSON

        Review savedReview = reviewRepository.save(review);

        // Update driver's rating
        updateDriverRating(ride.getDriver());

        // Send notification to driver
        notificationService.sendReviewReceivedNotification(savedReview);

        return savedReview;
    }

    // Update driver's average rating
    private void updateDriverRating(User driver) {
        Double avgRating = getAverageRating(driver);
        driver.setRating(avgRating);
        // Note: You might need to inject UserRepository to save this
    }

    // Get reviews for a user (as reviewee)
    public List<Review> getReviewsForUser(User user) {
        return reviewRepository.findByRevieweeOrderByCreatedAtDesc(user);
    }

    // Get reviews by a user (as reviewer)
    public List<Review> getReviewsByUser(User user) {
        return reviewRepository.findByReviewerOrderByCreatedAtDesc(user);
    }

    // Get average rating for a user
    public Double getAverageRating(User user) {
        Double avgRating = reviewRepository.getAverageRatingForUser(user);
        return avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0;
    }

    // Get total review count for a user
    public Long getTotalReviewCount(User user) {
        return reviewRepository.getTotalReviewsForUser(user);
    }

    // Check if user can review a booking
    public boolean canReview(Long bookingId, User user, ReviewType reviewType) {
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            return false;
        }

        Booking booking = bookingOpt.get();
        
        // Check if booking is completed (you might want to add a status check)
        // For now, we'll assume any confirmed booking can be reviewed
        
        // Check if review already exists
        if (reviewRepository.existsByBookingAndReviewType(booking, reviewType)) {
            return false;
        }

        // Check if user is authorized to give this type of review
        if (reviewType == ReviewType.PASSENGER_TO_DRIVER) {
            return booking.getPassenger().getId().equals(user.getId());
        } else {
            return booking.getRide().getDriver().getId().equals(user.getId());
        }
    }

    // Get review for a specific booking and review type
    public Optional<Review> getReview(Long bookingId, ReviewType reviewType) {
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            return Optional.empty();
        }
        
        return reviewRepository.findByBookingAndReviewType(bookingOpt.get(), reviewType);
    }

    // Get user rating summary
    public UserRatingSummary getUserRatingSummary(User user) {
        Double avgRating = getAverageRating(user);
        Long totalReviews = getTotalReviewCount(user);
        List<Review> recentReviews = reviewRepository.findByRevieweeOrderByCreatedAtDesc(user)
            .stream()
            .limit(5)
            .toList();
        
        return new UserRatingSummary(avgRating, totalReviews, recentReviews);
    }

    // Inner class for rating summary
    public static class UserRatingSummary {
        private Double averageRating;
        private Long totalReviews;
        private List<Review> recentReviews;

        public UserRatingSummary(Double averageRating, Long totalReviews, List<Review> recentReviews) {
            this.averageRating = averageRating;
            this.totalReviews = totalReviews;
            this.recentReviews = recentReviews;
        }

        // Getters
        public Double getAverageRating() { return averageRating; }
        public Long getTotalReviews() { return totalReviews; }
        public List<Review> getRecentReviews() { return recentReviews; }
    }
}
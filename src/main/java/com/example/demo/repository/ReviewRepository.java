package com.example.demo.repository;

import com.example.demo.entity.Review;
import com.example.demo.entity.ReviewType;
import com.example.demo.entity.User;
import com.example.demo.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    List<Review> findByRevieweeOrderByCreatedAtDesc(User reviewee);
    
    List<Review> findByReviewerOrderByCreatedAtDesc(User reviewer);
    
    Optional<Review> findByBookingAndReviewType(Booking booking, ReviewType reviewType);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewee = :user")
    Double getAverageRatingForUser(@Param("user") User user);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.reviewee = :user")
    Long getTotalReviewsForUser(@Param("user") User user);
    
    boolean existsByBookingAndReviewType(Booking booking, ReviewType reviewType);
}
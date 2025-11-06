package com.example.demo.dto;

import jakarta.validation.constraints.*;

public class ReviewRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @Size(max = 500, message = "Comment must not exceed 500 characters")
    private String comment;

    @NotBlank(message = "Review type is required")
    private String reviewType; // "PASSENGER_TO_DRIVER" or "DRIVER_TO_PASSENGER"

    private java.util.List<String> photos; // Base64 encoded photos

    // Constructors
    public ReviewRequest() {}

    public ReviewRequest(Long bookingId, Integer rating, String comment, String reviewType) {
        this.bookingId = bookingId;
        this.rating = rating;
        this.comment = comment;
        this.reviewType = reviewType;
    }

    // Getters and Setters
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getReviewType() { return reviewType; }
    public void setReviewType(String reviewType) { this.reviewType = reviewType; }

    public java.util.List<String> getPhotos() { return photos; }
    public void setPhotos(java.util.List<String> photos) { this.photos = photos; }
}

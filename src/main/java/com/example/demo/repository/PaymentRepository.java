package com.example.demo.repository;

import com.example.demo.entity.Payment;
import com.example.demo.entity.PaymentStatus;
import com.example.demo.entity.User;
import com.example.demo.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find payment by Razorpay payment ID
     */
    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    /**
     * Find payment by Razorpay order ID
     */
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    /**
     * Find all payments for a specific user
     */
    List<Payment> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Find all payments for a specific booking
     */
    List<Payment> findByBookingOrderByCreatedAtDesc(Booking booking);

    /**
     * Find payments by status
     */
    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);

    /**
     * Find payments by user and status
     */
    List<Payment> findByUserAndStatusOrderByCreatedAtDesc(User user, PaymentStatus status);

    /**
     * Find successful payments for a user within date range
     */
    @Query("SELECT p FROM Payment p WHERE p.user = :user AND p.status = 'SUCCESS' " +
           "AND p.paidAt BETWEEN :startDate AND :endDate ORDER BY p.paidAt DESC")
    List<Payment> findSuccessfulPaymentsByUserAndDateRange(
            @Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find pending payments older than specified time (for cleanup)
     */
    @Query("SELECT p FROM Payment p WHERE p.status IN ('CREATED', 'PENDING') " +
           "AND p.createdAt < :cutoffTime")
    List<Payment> findPendingPaymentsOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Get total amount paid by user
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.user = :user AND p.status = 'SUCCESS'")
    Double getTotalAmountPaidByUser(@Param("user") User user);

    /**
     * Get payment statistics for a date range
     */
    @Query("SELECT p.status, COUNT(p), COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.createdAt BETWEEN :startDate AND :endDate GROUP BY p.status")
    List<Object[]> getPaymentStatistics(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Check if booking has successful payment
     */
    @Query("SELECT COUNT(p) > 0 FROM Payment p WHERE p.booking = :booking AND p.status = 'SUCCESS'")
    boolean hasSuccessfulPayment(@Param("booking") Booking booking);

    /**
     * Find latest payment for a booking
     */
    Optional<Payment> findFirstByBookingOrderByCreatedAtDesc(Booking booking);

    /**
     * Admin queries for revenue calculation
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status")
    Double sumByStatus(@Param("status") PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status AND p.createdAt >= :date")
    Double sumByStatusAndCreatedAtAfter(@Param("status") PaymentStatus status, @Param("date") LocalDateTime date);
}

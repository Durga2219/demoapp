package com.example.demo.repository;

import com.example.demo.entity.EmailOtp;
import com.example.demo.entity.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {

    /**
     * Find the latest valid OTP for an email and type
     */
    @Query("SELECT e FROM EmailOtp e WHERE e.email = :email AND e.type = :type " +
           "AND e.verified = false AND e.expiresAt > :now ORDER BY e.createdAt DESC")
    Optional<EmailOtp> findLatestValidOtp(@Param("email") String email, 
                                         @Param("type") OtpType type, 
                                         @Param("now") LocalDateTime now);

    /**
     * Find OTP by email, OTP code, and type
     */
    Optional<EmailOtp> findByEmailAndOtpAndTypeAndVerifiedFalse(String email, String otp, OtpType type);

    /**
     * Delete expired OTPs (cleanup)
     */
    @Modifying
    @Query("DELETE FROM EmailOtp e WHERE e.expiresAt < :now")
    void deleteExpiredOtps(@Param("now") LocalDateTime now);

    /**
     * Mark all previous OTPs for email and type as verified (invalidate them)
     */
    @Modifying
    @Query("UPDATE EmailOtp e SET e.verified = true WHERE e.email = :email AND e.type = :type")
    void invalidateAllOtpsForEmailAndType(@Param("email") String email, @Param("type") OtpType type);

    /**
     * Count valid OTPs for email and type in last hour (rate limiting)
     */
    @Query("SELECT COUNT(e) FROM EmailOtp e WHERE e.email = :email AND e.type = :type " +
           "AND e.createdAt > :since")
    long countOtpsInLastHour(@Param("email") String email, 
                            @Param("type") OtpType type, 
                            @Param("since") LocalDateTime since);
}
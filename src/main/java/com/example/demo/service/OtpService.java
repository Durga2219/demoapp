package com.example.demo.service;

import com.example.demo.entity.EmailOtp;
import com.example.demo.entity.OtpType;
import com.example.demo.repository.EmailOtpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom random = new SecureRandom();
    private static final int MAX_OTPS_PER_HOUR = 5;

    @Autowired
    private EmailOtpRepository otpRepository;

    @Autowired
    private EmailService emailService;

    @Value("${otp.expiry.minutes:5}")
    private int otpExpiryMinutes;

    @Value("${otp.length:6}")
    private int otpLength;

    /**
     * Generate and send OTP
     */
    @Transactional
    public void generateAndSendOtp(String email, OtpType type) {
        // Rate limiting check
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long otpCount = otpRepository.countOtpsInLastHour(email, type, oneHourAgo);
        
        if (otpCount >= MAX_OTPS_PER_HOUR) {
            throw new RuntimeException("Too many OTP requests. Please try again after an hour.");
        }

        // Invalidate all previous OTPs for this email and type
        otpRepository.invalidateAllOtpsForEmailAndType(email, type);

        // Generate new OTP
        String otp = generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(otpExpiryMinutes);

        // Save OTP to database
        EmailOtp emailOtp = new EmailOtp(email, otp, expiresAt, type);
        otpRepository.save(emailOtp);

        // Send OTP email
        emailService.sendOtpEmail(email, otp, type.getDescription());

        logger.info("OTP generated and sent for email: {} type: {}", email, type);
    }

    /**
     * Verify OTP
     */
    @Transactional
    public boolean verifyOtp(String email, String otp, OtpType type) {
        Optional<EmailOtp> otpRecord = otpRepository.findByEmailAndOtpAndTypeAndVerifiedFalse(email, otp, type);

        if (otpRecord.isEmpty()) {
            logger.warn("Invalid OTP attempt for email: {} type: {}", email, type);
            return false;
        }

        EmailOtp emailOtp = otpRecord.get();

        if (emailOtp.isExpired()) {
            logger.warn("Expired OTP attempt for email: {} type: {}", email, type);
            return false;
        }

        // Mark OTP as verified
        emailOtp.setVerified(true);
        otpRepository.save(emailOtp);

        logger.info("OTP verified successfully for email: {} type: {}", email, type);
        return true;
    }

    /**
     * Check if there's a valid OTP for email and type
     */
    public boolean hasValidOtp(String email, OtpType type) {
        Optional<EmailOtp> otpRecord = otpRepository.findLatestValidOtp(email, type, LocalDateTime.now());
        return otpRecord.isPresent();
    }

    /**
     * Generate random OTP
     */
    private String generateOtp() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    /**
     * Cleanup expired OTPs (runs every 12 hours with 20 minute initial delay)
     */
    @Scheduled(fixedRate = 43200000, initialDelay = 1200000) // 12 hours, 20 min delay
    @Transactional
    public void cleanupExpiredOtps() {
        try {
            otpRepository.deleteExpiredOtps(LocalDateTime.now());
            logger.info("Expired OTPs cleaned up successfully");
        } catch (Exception e) {
            logger.error("Error cleaning up expired OTPs", e);
        }
    }
}
package com.ashu.ride_sharing.services;

import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ashu.ride_sharing.dto.request.LoginRequest;
import com.ashu.ride_sharing.dto.request.RefreshTokenRequest;
import com.ashu.ride_sharing.dto.request.UserRegistrationRequest;
import com.ashu.ride_sharing.dto.response.AuthResponse;
import com.ashu.ride_sharing.exception.EmailAlreadyExistsException;
import com.ashu.ride_sharing.exception.InvalidTokenException;
import com.ashu.ride_sharing.models.User;
import com.ashu.ride_sharing.models.VerificationToken;
import com.ashu.ride_sharing.models.enums.UserStatus;
import com.ashu.ride_sharing.repositories.UserRepository;
import com.ashu.ride_sharing.repositories.VerificationTokenRepository;
import com.ashu.ride_sharing.security.services.EmailService;
import com.ashu.ride_sharing.security.services.JWT_Service;
import com.ashu.ride_sharing.utils.CloudinaryUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWT_Service jwtService;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final CloudinaryUtils cloudinaryUtils;
    @Transactional
    public AuthResponse register(UserRegistrationRequest userRegistrationRequest){
        if (userRepository.existsByEmail(userRegistrationRequest.getEmail())) {
            log.warn("Registration attempt with existing email:{}", userRegistrationRequest.getEmail());
            throw new EmailAlreadyExistsException("Email address already in use");
        }

         String imageUrl = null;
        try {
            Map uploadResult = cloudinaryUtils.uploadImage(userRegistrationRequest.getProfilePicture());
            imageUrl = (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            log.error("Image upload failed", e);
            throw new RuntimeException("Profile picture upload failed. Please try again.");
        }

        var user = User.builder()
            .username(userRegistrationRequest.getUsername())
            .email(userRegistrationRequest.getEmail())
            .password(passwordEncoder.encode(userRegistrationRequest.getPassword()))
            .firstName(userRegistrationRequest.getFirstName())
            .lastName(userRegistrationRequest.getLastName())
            .role(userRegistrationRequest.getRole())
            .phoneNumber(userRegistrationRequest.getPhoneNumber())
            .profilePictureUrl(imageUrl)

            // 
            .status(UserStatus.ACTIVE)
    .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getEmail());


        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token,savedUser);
        tokenRepository.save(verificationToken);
        log.info("Verification token generated for user: {}",savedUser.getEmail());

        emailService.sendVerificationEmail(savedUser, token);

        return AuthResponse.builder()
        .message("Registration successfull. Please check your email to verify your account.")
        .build();
    }


    public AuthResponse login(LoginRequest request){
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(), 
                    request.getPassword()
                    )
                );
             log.info("Authentication successful for user:{}",request.getEmail());
        } catch (Exception e) {
            log.warn("Authentication failed for user{}:{}", request.getEmail(), e.getMessage());
            throw new BadCredentialsException("Invalid email or password");
        }

        var user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(()->new UsernameNotFoundException("User not found after sucessfull authentication: "+request.getEmail()));

        if (user!=null) {
            log.info("User found: {}", request.getEmail());
        }

        if (!user.isEnabled()) {
            log.warn("Login attempt by disabled user: {}", request.getEmail());
            throw new BadCredentialsException("Account not verified. Please check your email");
        }

        var accessToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

                log.info("JWT tokens generated for user: {}", user.getEmail());


        return AuthResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .message("Login successful")
        .build();    
    }


    @Transactional
    public String verifyEmail(String token){
        VerificationToken verificationToken = tokenRepository.findByToken(token)
        .orElseThrow(()-> new InvalidTokenException("Invalid verification token."));

        if(verificationToken.isExpired()){
            tokenRepository.delete(verificationToken);
            log.warn("Attempt to verify email with expired token : {}",token);
            throw new InvalidTokenException("Verification token has expired.");
        }

        User user = verificationToken.getUser();
        if (user == null) {
            log.error("Verification token {} has no associated user.", token);
            tokenRepository.delete(verificationToken);
            throw new InvalidTokenException("Invalid verification token state.");
        }


        if (user.isEnabled()) {
            log.info("User {} attempted to verify email but is already enabled.", user.getEmail());
            tokenRepository.delete(verificationToken);
            return "Account already verified";
        }

        user.setEmailVerified(true);
        userRepository.save(user);
        log.info("User {} enabled successfully.", user.getEmail());

        tokenRepository.delete(verificationToken);
        log.info("Verification token {} deleted.", token);

        return "Email verified successfully! You can now log in.";
    }



    public AuthResponse refreshToken(RefreshTokenRequest request){
        String refreshToken = request.getRefreshToken();
        try {
            String userEmail = jwtService.extractUsername(refreshToken);
            UserDetails userDetails = userRepository.findByEmail(userEmail)
            .orElseThrow(()->new UsernameNotFoundException("User not found for refresh token"));
            
            if (jwtService.isTokenValid(refreshToken, userDetails)) {
                String newAccessToken = jwtService.generateToken(userDetails);
                String newRefreshToken = jwtService.generateRefreshToken(userDetails);
                log.info("Access token refreshed for user: {}", userEmail);

                return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .message("Token refreshed successfully")
                .build();

            }else{
                log.warn("Invalid refresh token provided for user: {}", userEmail);
                throw new InvalidTokenException("Invalid refresh token.");
            }

        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            throw new InvalidTokenException("Could not refresh token: " + e.getMessage());
        }
    }
}

package com.example.demo.service;

import com.example.demo.controller.AdminWebSocketController;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminWebSocketController adminWebSocketController;

    /**
     * Find user by username
     */
    public User findByUsername(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        return userOpt.orElse(null);
    }

    /**
     * Find user by email
     */
    public User findByEmail(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        return userOpt.orElse(null);
    }

    /**
     * Find user by ID
     */
    public User findById(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        return userOpt.orElse(null);
    }

    /**
     * Check if user exists by username
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Check if user exists by email
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Save user
     */
    public User save(User user) {
        User savedUser = userRepository.save(user);
        
        // Notify admin of new user registration
        try {
            adminWebSocketController.notifyNewUser(savedUser);
        } catch (Exception e) {
            logger.warn("Failed to send admin notification for new user: " + e.getMessage());
        }
        
        return savedUser;
    }

    /**
     * Update user profile
     */
    public User updateProfile(User user) {
        return userRepository.save(user);
    }
}
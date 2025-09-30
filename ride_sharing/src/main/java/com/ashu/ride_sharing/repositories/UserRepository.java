package com.ashu.ride_sharing.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ashu.ride_sharing.models.User;

public interface UserRepository extends JpaRepository<User,UUID>{
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
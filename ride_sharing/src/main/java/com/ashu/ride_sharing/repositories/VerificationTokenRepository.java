package com.ashu.ride_sharing.repositories;


import org.springframework.data.jpa.repository.JpaRepository;

import com.ashu.ride_sharing.models.VerificationToken;

import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
    void deleteByUser_Id(UUID userId);

}
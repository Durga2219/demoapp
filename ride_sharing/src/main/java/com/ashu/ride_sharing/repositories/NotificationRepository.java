package com.ashu.ride_sharing.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ashu.ride_sharing.models.Notification;
import com.ashu.ride_sharing.models.enums.NotificationStatus;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUser_IdOrderByCreatedAtDesc(UUID userId);
    
    List<Notification> findByUser_IdAndStatusOrderByCreatedAtDesc(UUID userId, NotificationStatus status);
    
    long countByUser_IdAndStatus(UUID userId, NotificationStatus status);
}

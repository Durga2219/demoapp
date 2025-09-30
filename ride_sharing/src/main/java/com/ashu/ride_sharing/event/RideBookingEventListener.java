package com.ashu.ride_sharing.event;

import com.ashu.ride_sharing.models.Notification;
import com.ashu.ride_sharing.models.Ride;
import com.ashu.ride_sharing.models.User;
import com.ashu.ride_sharing.models.enums.NotificationStatus;
import com.ashu.ride_sharing.models.enums.NotificationType;
import com.ashu.ride_sharing.repositories.NotificationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RideBookingEventListener {

    private final NotificationRepository notificationRepository;

    @Async
    @EventListener
    public void handleRideBookedEvent(RideBookedEvent event) {
        Ride ride = event.getRide();
        User driver = ride.getDriver();
        
        // Create and save notification
        Notification notification = Notification.builder()
                .user(driver)
                .title("New Ride Request")
                .message(String.format("You have a new ride request from %s", 
                        event.getPassenger().getFullName()))
                .type(NotificationType.RIDE_REQUEST)
                .status(NotificationStatus.UNREAD)
                .relatedEntityId(ride.getRideId().toString())
                .relatedEntityType("BOOKING")
                .build();
        
        notificationRepository.save(notification);
    }
}

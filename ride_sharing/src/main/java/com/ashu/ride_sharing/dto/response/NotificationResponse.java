package com.ashu.ride_sharing.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ashu.ride_sharing.models.enums.NotificationStatus;
import com.ashu.ride_sharing.models.enums.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private UUID notificationId;
    private String title;
    private String message;
    private NotificationType type;
    private NotificationStatus status;
    private String relatedEntityType;
    private String relatedEntityId;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}

package com.abreha.government_benefits.notification;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NotificationResponse {

    private final Long id;
    private final Long userId;
    private final Long applicationId;
    private final String title;
    private final String message;
    private final boolean read;
    private final LocalDateTime createdAt;
    private final LocalDateTime readAt;

    public NotificationResponse(Notification notification) {
        this.id = notification.getId();
        this.userId = notification.getUserId();
        this.applicationId = notification.getApplicationId();
        this.title = notification.getTitle();
        this.message = notification.getMessage();
        this.read = notification.isRead();
        this.createdAt = notification.getCreatedAt();
        this.readAt = notification.getReadAt();
    }
}

package com.abreha.government_benefits.notification;

import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(
            NotificationService notificationService
    ) {
        this.notificationService = notificationService;
    }

    @GetMapping("/user/{userId}")
    public List<NotificationResponse> getUserNotifications(
            @PathVariable Long userId
    ) {
        return notificationService.getUserNotifications(userId);
    }

    @GetMapping("/user/{userId}/unread-count")
    public long getUnreadCount(@PathVariable Long userId) {
        return notificationService.getUnreadCount(userId);
    }

    @PutMapping("/{notificationId}/read")
    public NotificationResponse markAsRead(
            @PathVariable Long notificationId,
            @RequestParam Long userId
    ) {
        return notificationService.markAsRead(notificationId, userId);
    }

    @GetMapping("/mine")
    public List<NotificationResponse> getMyNotifications(
            Principal principal
    ) {
        return notificationService.getMyNotifications(
                principal.getName()
        );
    }

    @GetMapping("/mine/unread-count")
    public long getMyUnreadCount(Principal principal) {
        return notificationService.getMyUnreadCount(
                principal.getName()
        );
    }

    @PutMapping("/mine/{notificationId}/read")
    public NotificationResponse markMyNotificationAsRead(
            @PathVariable Long notificationId,
            Principal principal
    ) {
        return notificationService.markMyNotificationAsRead(
                notificationId,
                principal.getName()
        );
    }
}
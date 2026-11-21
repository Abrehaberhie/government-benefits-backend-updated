package com.abreha.government_benefits.notification;

import com.abreha.government_benefits.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public NotificationResponse createNotification(
            Long userId,
            Long applicationId,
            String title,
            String message
    ) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setApplicationId(applicationId);
        notification.setTitle(title);
        notification.setMessage(message);

        Notification saved = notificationRepository.save(notification);

        return new NotificationResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(Long userId) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDescIdDesc(userId)
                .stream()
                .map(NotificationResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    public NotificationResponse markAsRead(
            Long notificationId,
            Long userId
    ) {
        Notification notification = notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Notification not found"
                ));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
        }

        return new NotificationResponse(
                notificationRepository.save(notification)
        );
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(String email) {
        Long userId = findUserIdByEmail(email);
        return getUserNotifications(userId);
    }

    @Transactional(readOnly = true)
    public long getMyUnreadCount(String email) {
        Long userId = findUserIdByEmail(email);
        return getUnreadCount(userId);
    }

    public NotificationResponse markMyNotificationAsRead(
            Long notificationId,
            String email
    ) {
        Long userId = findUserIdByEmail(email);
        return markAsRead(notificationId, userId);
    }

    private Long findUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ))
                .getId();
    }
}
package com.chirag.shopsphere.service;

import com.chirag.shopsphere.dto.NotificationCreateRequest;
import com.chirag.shopsphere.dto.NotificationResponse;
import com.chirag.shopsphere.dto.NotificationStatusUpdateRequest;
import com.chirag.shopsphere.dto.PageResponse;
import com.chirag.shopsphere.entity.Notification;
import com.chirag.shopsphere.entity.NotificationStatus;
import com.chirag.shopsphere.entity.User;
import com.chirag.shopsphere.exception.ResourceNotFoundException;
import com.chirag.shopsphere.repository.NotificationRepository;
import com.chirag.shopsphere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public NotificationResponse createNotification(NotificationCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

        Notification notification = Notification.builder()
                .user(user)
                .type(request.getType())
                .subject(request.getSubject())
                .message(request.getMessage())
                .status(NotificationStatus.PENDING)
                .build();

        Notification saved = notificationRepository.save(notification);
        return NotificationResponse.fromEntity(saved);
    }

    public NotificationResponse getNotificationById(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
        return NotificationResponse.fromEntity(notification);
    }

    public PageResponse<NotificationResponse> listNotifications(Pageable pageable) {
        Page<NotificationResponse> page = notificationRepository.findAll(pageable)
                .map(NotificationResponse::fromEntity);
        return PageResponse.from(page);
    }

    public PageResponse<NotificationResponse> listNotificationsByUser(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        Page<NotificationResponse> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::fromEntity);
        return PageResponse.from(page);
    }

    @Transactional
    public NotificationResponse updateNotificationStatus(Long id, NotificationStatusUpdateRequest request) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));

        notification.setStatus(request.getStatus());
        if (request.getStatus() == NotificationStatus.SENT) {
            notification.setSentAt(LocalDateTime.now());
        }

        Notification updated = notificationRepository.save(notification);
        return NotificationResponse.fromEntity(updated);
    }
}

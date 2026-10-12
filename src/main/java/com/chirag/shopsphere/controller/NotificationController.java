package com.chirag.shopsphere.controller;

import com.chirag.shopsphere.dto.NotificationCreateRequest;
import com.chirag.shopsphere.dto.NotificationResponse;
import com.chirag.shopsphere.dto.NotificationStatusUpdateRequest;
import com.chirag.shopsphere.dto.PageResponse;
import com.chirag.shopsphere.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(
            @Valid @RequestBody NotificationCreateRequest request) {
        NotificationResponse response = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getNotificationById(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.getNotificationById(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> listNotifications(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(notificationService.listNotifications(pageable));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PageResponse<NotificationResponse>> listNotificationsByUser(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(notificationService.listNotificationsByUser(userId, pageable));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<NotificationResponse> updateNotificationStatus(
            @PathVariable Long id,
            @Valid @RequestBody NotificationStatusUpdateRequest request) {
        return ResponseEntity.ok(notificationService.updateNotificationStatus(id, request));
    }
}

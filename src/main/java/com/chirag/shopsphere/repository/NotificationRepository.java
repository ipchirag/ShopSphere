package com.chirag.shopsphere.repository;

import com.chirag.shopsphere.entity.Notification;
import com.chirag.shopsphere.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserId(Long userId);

    Page<Notification> findByUserId(Long userId, Pageable pageable);

    List<Notification> findByStatus(NotificationStatus status);

    Page<Notification> findByStatus(NotificationStatus status, Pageable pageable);

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}

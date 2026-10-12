package com.chirag.shopsphere.dto;

import com.chirag.shopsphere.entity.NotificationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationStatusUpdateRequest {

    @NotNull(message = "Notification status is required (PENDING, SENT, FAILED)")
    private NotificationStatus status;
}

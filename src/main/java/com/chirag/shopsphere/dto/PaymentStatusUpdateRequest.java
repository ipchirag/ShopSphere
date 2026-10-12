package com.chirag.shopsphere.dto;

import com.chirag.shopsphere.entity.PaymentStatus;
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
public class PaymentStatusUpdateRequest {

    @NotNull(message = "Payment status is required (PENDING, SUCCESS, FAILED, REFUNDED)")
    private PaymentStatus status;
}

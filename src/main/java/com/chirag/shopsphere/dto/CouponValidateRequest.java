package com.chirag.shopsphere.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponValidateRequest {

    @NotNull(message = "Order amount is required")
    @PositiveOrZero(message = "Order amount must be zero or positive")
    private BigDecimal orderAmount;
}

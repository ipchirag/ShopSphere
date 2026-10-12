package com.chirag.shopsphere.dto;

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
public class CouponValidateResponse {

    private boolean valid;
    private String code;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String message;
}

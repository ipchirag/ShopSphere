package com.chirag.shopsphere.service;

import com.chirag.shopsphere.dto.CouponRequest;
import com.chirag.shopsphere.dto.CouponResponse;
import com.chirag.shopsphere.dto.CouponValidateRequest;
import com.chirag.shopsphere.dto.CouponValidateResponse;
import com.chirag.shopsphere.dto.PageResponse;
import com.chirag.shopsphere.entity.Coupon;
import com.chirag.shopsphere.entity.DiscountType;
import com.chirag.shopsphere.exception.DuplicateResourceException;
import com.chirag.shopsphere.exception.ResourceNotFoundException;
import com.chirag.shopsphere.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional
    public CouponResponse createCoupon(CouponRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (couponRepository.existsByCode(code)) {
            throw new DuplicateResourceException("Coupon", "code", code);
        }

        Coupon coupon = Coupon.builder()
                .code(code)
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minimumOrderAmount(request.getMinimumOrderAmount())
                .expiresAt(request.getExpiresAt())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        Coupon saved = couponRepository.save(coupon);
        return CouponResponse.fromEntity(saved);
    }

    public CouponResponse getCouponById(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
        return CouponResponse.fromEntity(coupon);
    }

    public PageResponse<CouponResponse> listCoupons(Pageable pageable) {
        Page<CouponResponse> page = couponRepository.findAll(pageable)
                .map(CouponResponse::fromEntity);
        return PageResponse.from(page);
    }

    @Transactional
    public CouponResponse updateCoupon(Long id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));

        String newCode = request.getCode().trim().toUpperCase();
        if (!coupon.getCode().equalsIgnoreCase(newCode) && couponRepository.existsByCode(newCode)) {
            throw new DuplicateResourceException("Coupon", "code", newCode);
        }

        coupon.setCode(newCode);
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinimumOrderAmount(request.getMinimumOrderAmount());
        coupon.setExpiresAt(request.getExpiresAt());
        if (request.getActive() != null) {
            coupon.setActive(request.getActive());
        }

        Coupon updated = couponRepository.save(coupon);
        return CouponResponse.fromEntity(updated);
    }

    @Transactional
    public void deactivateCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
        coupon.setActive(false);
        couponRepository.save(coupon);
    }

    public CouponValidateResponse validateCoupon(String code, CouponValidateRequest request) {
        String cleanCode = code.trim().toUpperCase();
        Coupon coupon = couponRepository.findByCode(cleanCode)
                .orElse(null);

        if (coupon == null) {
            return CouponValidateResponse.builder()
                    .valid(false)
                    .code(cleanCode)
                    .discountAmount(BigDecimal.ZERO)
                    .finalAmount(request.getOrderAmount())
                    .message("Coupon does not exist")
                    .build();
        }

        if (!coupon.isActive()) {
            return CouponValidateResponse.builder()
                    .valid(false)
                    .code(cleanCode)
                    .discountAmount(BigDecimal.ZERO)
                    .finalAmount(request.getOrderAmount())
                    .message("Coupon is inactive")
                    .build();
        }

        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(LocalDateTime.now())) {
            return CouponValidateResponse.builder()
                    .valid(false)
                    .code(cleanCode)
                    .discountAmount(BigDecimal.ZERO)
                    .finalAmount(request.getOrderAmount())
                    .message("Coupon has expired")
                    .build();
        }

        if (coupon.getMinimumOrderAmount() != null && request.getOrderAmount().compareTo(coupon.getMinimumOrderAmount()) < 0) {
            return CouponValidateResponse.builder()
                    .valid(false)
                    .code(cleanCode)
                    .discountAmount(BigDecimal.ZERO)
                    .finalAmount(request.getOrderAmount())
                    .message("Order amount is below minimum required: " + coupon.getMinimumOrderAmount())
                    .build();
        }

        BigDecimal discount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = request.getOrderAmount().multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    .min(request.getOrderAmount());
        } else {
            discount = coupon.getDiscountValue().min(request.getOrderAmount());
        }

        BigDecimal finalAmount = request.getOrderAmount().subtract(discount).max(BigDecimal.ZERO);

        return CouponValidateResponse.builder()
                .valid(true)
                .code(cleanCode)
                .discountAmount(discount)
                .finalAmount(finalAmount)
                .message("Coupon is valid and applied successfully")
                .build();
    }
}

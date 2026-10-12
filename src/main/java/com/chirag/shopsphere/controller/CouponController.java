package com.chirag.shopsphere.controller;

import com.chirag.shopsphere.dto.CouponRequest;
import com.chirag.shopsphere.dto.CouponResponse;
import com.chirag.shopsphere.dto.CouponValidateRequest;
import com.chirag.shopsphere.dto.CouponValidateResponse;
import com.chirag.shopsphere.dto.PageResponse;
import com.chirag.shopsphere.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody CouponRequest request) {
        CouponResponse response = couponService.createCoupon(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CouponResponse> getCouponById(@PathVariable Long id) {
        return ResponseEntity.ok(couponService.getCouponById(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<CouponResponse>> listCoupons(
            @PageableDefault(size = 20, sort = "code") Pageable pageable) {
        return ResponseEntity.ok(couponService.listCoupons(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CouponResponse> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(couponService.updateCoupon(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateCoupon(@PathVariable Long id) {
        couponService.deactivateCoupon(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{code}/validate")
    public ResponseEntity<CouponValidateResponse> validateCoupon(
            @PathVariable String code,
            @Valid @RequestBody CouponValidateRequest request) {
        return ResponseEntity.ok(couponService.validateCoupon(code, request));
    }
}

package com.example.nomodel.coupon.application.service;

import com.example.nomodel.coupon.application.dto.request.CouponCreateRequest;
import com.example.nomodel.coupon.application.dto.response.CouponResponse;
import com.example.nomodel.coupon.domain.model.Coupon;
import com.example.nomodel.coupon.domain.model.CouponStatus;
import com.example.nomodel.coupon.domain.model.DiscountType;
import com.example.nomodel.coupon.domain.service.CouponDomainService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CouponService {

    private final CouponDomainService domainService;

    public CouponService(CouponDomainService domainService) {
        this.domainService = domainService;
    }

    public CouponResponse createCoupon(CouponCreateRequest request) {
        Coupon coupon = new Coupon(
                request.getCouponCode(),
                request.getName(),
                request.getDiscountType(),
                request.getDiscountValue(),
                request.getValidFrom(),
                request.getValidUntil()
        );
        Coupon saved = domainService.createCoupon(coupon);
        return toResponse(saved);
    }

    public List<CouponResponse> getCouponsByMember(Long memberId) {
        return domainService.getCouponsByMember(memberId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CouponResponse applyCoupon(String couponCode, Long memberId) {
        Coupon coupon = domainService.applyCoupon(couponCode, memberId);
        return toResponse(coupon);
    }

    private CouponResponse toResponse(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCouponCode(),
                coupon.getName(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getValidFrom(),
                coupon.getValidUntil(),
                coupon.getStatus(),
                coupon.getUsedAt(),
                coupon.getUsedBy()
        );
    }
}

package com.example.nomodel.coupon.application.dto.response;

import com.example.nomodel.coupon.domain.model.CouponStatus;
import com.example.nomodel.coupon.domain.model.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CouponResponse {
    private Long id;
    private String couponCode;
    private String name;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private CouponStatus status;
    private LocalDateTime usedAt;
    private Long usedBy;

    public CouponResponse(Long id, String couponCode, String name,
                          DiscountType discountType, BigDecimal discountValue,
                          LocalDateTime validFrom, LocalDateTime validUntil,
                          CouponStatus status, LocalDateTime usedAt, Long usedBy) {
        this.id = id;
        this.couponCode = couponCode;
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.status = status;
        this.usedAt = usedAt;
        this.usedBy = usedBy;
    }

    // getter...
}

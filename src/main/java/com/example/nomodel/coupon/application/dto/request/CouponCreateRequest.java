package com.example.nomodel.coupon.application.dto.request;

import com.example.nomodel.coupon.domain.model.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CouponCreateRequest {
    private String couponCode;
    private String name;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;

    public String getCouponCode() { return couponCode; }
    public String getName() { return name; }
    public DiscountType getDiscountType() { return discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public LocalDateTime getValidUntil() { return validUntil; }
}
